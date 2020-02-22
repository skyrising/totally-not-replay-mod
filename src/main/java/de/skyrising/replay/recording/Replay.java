package de.skyrising.replay.recording;

import de.skyrising.replay.Utils;
import de.skyrising.replay.event.Event;
import de.skyrising.replay.event.MetadataEvent;
import de.skyrising.replay.mixin.accessors.MinecraftClientAccessor;
import de.skyrising.replay.world.ReplayNetworkHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.Bootstrap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.PacketByteBuf;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.function.Consumer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Replay extends ReplayContext implements AutoCloseable {
    private final File filePath;
    private final RandomAccessFile file;
    private Inflater inflater;

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
        ReplayNetworkHandler netHandler = new ReplayNetworkHandler(client, client.currentScreen, this);
        ((MinecraftClientAccessor) client).setClientConnection(netHandler.getConnection());
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
        if (compressed && compressionSettings != null) {
            switch (compressionSettings.getCompressionType()) {
                case ZLIB: {
                    uncompressed = new byte[length];
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
                    break;
                }
                default: {
                    uncompressed = compressedBuffer;
                }
            }
        } else {
            uncompressed = compressedBuffer;
        }
        PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(uncompressed));
        try {
            return Event.read(networkState, buf);
        } finally {
            buf.release();
        }
    }

    @Nullable
    public Event readEvent() throws IOException {
        Event e = readEvent0();
        if (e != null) onEvent(e);
        return e;
    }

    public void readUntil(long endTimeCode, Consumer<Event> callback) throws IOException {
        while (true) {
            long offset = file.getFilePointer();
            Event e = readEvent0();
            if (e == null) return;
            if (e.timecode <= endTimeCode) {
                onEvent(e);
                callback.accept(e);
            } else {
                file.seek(offset);
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
        Replay replay = new Replay(new File("recordings/2019-12-24_11-43-28.tnr"), "r");
        while (replay.hasNext()) {
            System.out.println(replay.readEvent());
        }
    }
}
