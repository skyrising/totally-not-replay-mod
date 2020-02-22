package de.skyrising.replay.event;

import net.minecraft.network.NetworkState;
import net.minecraft.util.PacketByteBuf;

public class CompressionSettingEvent extends Event {
    private CompressionType compressionType;

    CompressionSettingEvent(long timecode, NetworkState networkState) {
        super(timecode, networkState);
    }

    public CompressionSettingEvent(long timecode, NetworkState networkState, CompressionType type) {
        super(timecode, networkState);
        this.compressionType = type;
    }

    @Override
    public Type getType() {
        return Type.COMPRESSION;
    }

    @Override
    public void read(PacketByteBuf buf) {
        this.compressionType = CompressionType.values[buf.readVarInt()];
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeVarInt(compressionType.ordinal());
    }

    public CompressionType getCompressionType() {
        return compressionType;
    }

    @Override
    public String toString() {
        return "CompressionSettingEvent{" +
            "compressionType=" + compressionType +
            ", timecode=" + timecode +
            '}';
    }

    public enum CompressionType {
        NONE, ZLIB;

        static CompressionType[] values = values();
    }
}
