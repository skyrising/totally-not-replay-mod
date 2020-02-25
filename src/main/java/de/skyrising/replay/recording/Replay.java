package de.skyrising.replay.recording;

import de.skyrising.replay.Utils;
import de.skyrising.replay.event.Event;
import de.skyrising.replay.event.MetadataEvent;
import de.skyrising.replay.mixin.accessors.MinecraftClientAccessor;
import de.skyrising.replay.world.ReplayConnection;
import de.skyrising.replay.world.ReplayNetworkHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.Bootstrap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.util.PacketByteBuf;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Replay extends ReplayContext implements AutoCloseable {
    private final File filePath;
    private final RandomAccessFile file;
    private Inflater inflater;
    private Queue<Event> buffered = new ArrayDeque<>();

    public Replay(File file, String mode) throws FileNotFoundException {
        this.filePath = file;
        this.file = new RandomAccessFile(file, mode);
    }

    public ReplaySummary readSummary() throws IOException {
        long length = file.length();
        file.seek(length - 4);
        file.seek(length - file.readInt() - 4);
        Event lastEvent = readEvent0();
        if (lastEvent instanceof MetadataEvent) {
            MetadataEvent meta = (MetadataEvent) lastEvent;
            System.out.println(meta.data);
        }
        return new ReplaySummary(filePath, length, lastEvent.timecode);
    }

    public void load() {
        MinecraftClient client = MinecraftClient.getInstance();
        ((MinecraftClientAccessor) client).setClientConnection(new ReplayConnection(this, client.currentScreen));
    }

    public boolean hasNext() throws IOException {
        return file.getFilePointer() < file.length() - 4;
    }

    @Nullable
    private Event readEvent0() throws IOException {
        if (!hasNext()) return null;
        int length = readVarInt();
        int compressedLength = readVarInt();
        boolean compressed = compressedLength != 0;
        if (!compressed) compressedLength = length;
        byte[] compressedBuffer = new byte[compressedLength];
        file.readFully(compressedBuffer);
        byte[] uncompressed;
        uncompressed = decompress(length, compressed, compressedBuffer);
        PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(uncompressed));
        try {
            return Event.read(networkState, buf);
        } catch (RuntimeException e) {
            buf.resetReaderIndex();
            System.err.println(Utils.hexdump(buf));
            throw e;
        } finally {
            buf.release();
        }
    }

    private byte[] decompress(int length, boolean compressed, byte[] compressedBuffer) throws IOException {
        if (!compressed || compressionSettings == null) return compressedBuffer;
        switch (compressionSettings.getCompressionType()) {
            case ZLIB: {
                byte[] uncompressed = new byte[length];
                inflater.setInput(compressedBuffer);
                try {
                    int uncompressedLength = inflater.inflate(uncompressed);
                    if (uncompressedLength != length) {
                        System.err.println("Uncompressed length is incorrect (" + uncompressedLength + " actual vs. " + length +")");
                        uncompressed = Utils.resize(uncompressed, uncompressedLength);
                        if (uncompressedLength > length) {
                            inflater.inflate(uncompressed, length, uncompressedLength - length);
                        }
                    }
                } catch (DataFormatException e) {
                    throw new IOException(e);
                }
                return uncompressed;
            }
        }
        return compressedBuffer;
    }

    @Nullable
    public Event readEvent() throws IOException {
        Event e = readEvent0();
        if (e != null) onEvent(e);
        return e;
    }

    public void readUntil(long endTimeCode, Consumer<Event> callback) throws IOException {
        while (!buffered.isEmpty()) {
            Event e = buffered.peek();
            if (e.timecode > endTimeCode) return;
            onEvent(e);
            callback.accept(buffered.remove());
        }
        while (true) {
            Event e = readEvent0();
            if (e == null) return;
            if (e.timecode <= endTimeCode) {
                onEvent(e);
                callback.accept(e);
            } else {
                buffered.add(e);
                System.out.println("Buffered " + e);
                return;
            }
        }
    }

    @Override
    protected void onCompressionUpdate() {
        if (inflater != null) {
            inflater.end();
            inflater = null;
        }
        switch (compressionSettings.getCompressionType()) {
            case NONE: break;
            case ZLIB: {
                inflater = new Inflater();
                break;
            }
        }
    }

    private int readVarInt() throws IOException {
        int result = 0;
        int bytesRead = 0;
        byte b;
        do {
            b = file.readByte();
            result |= (b & 0x7f) << (7 * bytesRead++);
            if (bytesRead > 5) {
                throw new RuntimeException("VarInt too big");
            }
        } while((b & 0x80) != 0);

        return result;
    }

    @Override
    public void close() throws IOException {
        if (inflater != null) inflater.end();
        file.close();
    }

    public static void main(String[] args) throws IOException {
        Bootstrap.initialize();
        // Replay replay = new Replay(new RandomAccessFile(new File("recordings/2019-12-24_12-14-36.tnr"), "r"));
        Replay replay = new Replay(new File("recordings/2020-02-23_16-05-30.tnr"), "r");
        int i = 0;
        while (replay.hasNext() && i++ < 100) {
            System.out.println(replay.readEvent());
        }
    }
}
