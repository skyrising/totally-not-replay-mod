package de.skyrising.replay.recording;

import com.google.gson.JsonObject;
import de.skyrising.replay.TotallyNotReplayMod;
import de.skyrising.replay.Utils;
import de.skyrising.replay.event.CompressionSettingEvent;
import de.skyrising.replay.event.Event;
import de.skyrising.replay.event.MetadataEvent;
import de.skyrising.replay.event.PacketEvent;
import io.netty.buffer.Unpooled;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import net.minecraft.util.PacketByteBuf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.zip.Deflater;

public class Recording extends ReplayContext implements AutoCloseable {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final SimpleDateFormat FILE_NAME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");

    public final RecordingSettings settings;

    private static boolean shouldRecord = true;
    public static Recording clientRecording;

    private final Set<Class<? extends Packet<?>>> ignoredPackets;
    private final SnapshotTracker snapshotTracker = new SnapshotTracker();
    private boolean started;
    private boolean stopped;
    private DataOutputStream outputStream;
    private long startTime;
    private Deflater deflater;
    private long offset;

    public Recording(RecordingSettings settings) {
        this.settings = settings;
        this.ignoredPackets = settings.getIgnoredPackets();
    }

    public synchronized void start() throws IOException {
        if (!started && !stopped) {
            started = true;
            startTime = System.currentTimeMillis();
            File outputFile = new File(TotallyNotReplayMod.getRecordingsFolder(), FILE_NAME_FORMAT.format(new Date(startTime)) + ".tnr");
            outputStream = new DataOutputStream(new FileOutputStream(outputFile));
            setCompression(CompressionSettingEvent.CompressionType.ZLIB);
        }
    }

    public synchronized void stop() throws IOException {
        if (started && !stopped) {
            stopped = true;
            System.out.println(snapshotTracker);
            if (deflater != null) deflater.end();
            JsonObject metadata = new JsonObject();
            long metadataOffset = offset;
            recordEvent(new MetadataEvent(getCurrentTime(), networkState, metadata), false);
            int offsetToEnd = (int) (offset - metadataOffset);
            outputStream.writeInt(offsetToEnd);
            outputStream.close();
        }
    }

    public boolean isActive() {
        return started && !stopped;
    }

    public long getCurrentTime() {
        return System.currentTimeMillis() - startTime;
    }

    public static void onClientPacket(Packet<?> packet) {
        if (clientRecording != null && clientRecording.isActive()) clientRecording.onPacket(packet);
    }

    public void onPacket(Packet<?> packet) {
        if (ignoredPackets.contains(packet.getClass())) return;
        try {
            this.recordEvent(new PacketEvent(this.getCurrentTime(), networkState, packet));
        } catch (IOException e) {
            LOGGER.warn("Could not record packet "+ packet, e);
        }
    }

    public void setCompression(CompressionSettingEvent.CompressionType type) throws IOException {
        recordEvent(new CompressionSettingEvent(getCurrentTime(), networkState, type));
    }

    @Override
    protected void onCompressionUpdate() {
        if (deflater != null) {
            deflater.end();
            deflater = null;
        }
        switch (compressionSettings.getCompressionType()) {
            case NONE: break;
            case ZLIB: {
                deflater = new Deflater();
                break;
            }
        }
    }

    public void recordEvent(Event e) throws IOException {
        recordEvent(e, e.getType() != Event.Type.COMPRESSION);
    }

    public void recordEvent(Event e, boolean allowCompression) throws IOException {
        snapshotTracker.onEvent(e, offset);
        onEvent(e);
        PacketByteBuf encoded = encode(e);
        PacketByteBuf framed = new PacketByteBuf(Unpooled.buffer(encoded.writerIndex() + 10));
        if (shouldCompress(encoded, allowCompression)) {
            writeCompressedEvent(encoded, framed);
        } else {
            framed.writeVarInt(encoded.writerIndex());
            framed.writeVarInt(0);
            framed.writeBytes(encoded);
        }
        encoded.release();
        byte[] framedBytes = framed.array();
        synchronized (this) {
            outputStream.write(framedBytes, 0, framed.writerIndex());
        }
        offset += framed.writerIndex();
        framed.release();
    }

    private boolean shouldCompress(PacketByteBuf encoded, boolean allowCompression) {
        if (!allowCompression || compressionSettings == null) return false;
        return compressionSettings.getCompressionType() != CompressionSettingEvent.CompressionType.NONE;
    }

    private void writeCompressedEvent(PacketByteBuf encoded, PacketByteBuf framed) {
        int uncompressedSize = encoded.writerIndex();
        byte[] bytes = encoded.array();
        byte[] compressed = new byte[uncompressedSize];
        int compressedSize;
        switch (compressionSettings.getCompressionType()) {
            case ZLIB: {
                deflater.setInput(bytes, 0, uncompressedSize);
                compressedSize = deflater.deflate(compressed, 0, compressed.length, Deflater.SYNC_FLUSH);
                while (compressedSize == compressed.length) {
                    compressed = Utils.resize(compressed, compressedSize * 2);
                    compressedSize += deflater.deflate(compressed, compressedSize, compressed.length - compressedSize, Deflater.SYNC_FLUSH);
                }
                break;
            }
            default: throw new UnsupportedOperationException();
        }
        framed.writeVarInt(uncompressedSize);
        framed.writeVarInt(compressedSize);
        framed.writeBytes(compressed, 0, compressedSize);
    }

    @Override
    public void close() throws Exception {
        this.stop();
    }

    private static PacketByteBuf encode(Event e) throws IOException {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        Event.write(buf, e);
        return buf;
    }

    public static void enableRecording(boolean enable) {
        shouldRecord = enable;
    }

    public static boolean shouldRecord(ClientConnection connection) {
        if (!shouldRecord) return false;
        shouldRecord = false;
        return true;
    }
}
