package de.skyrising.replay.event;

import net.minecraft.network.NetworkState;
import net.minecraft.util.PacketByteBuf;

import java.io.IOException;

public class SnapshotEvent extends Event {
    SnapshotEvent(long timecode, NetworkState state) {
        super(timecode, state);
    }

    @Override
    public Type getType() {
        return Type.SNAPSHOT;
    }

    @Override
    public void read(PacketByteBuf buf) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(PacketByteBuf buf) throws IOException {
        throw new UnsupportedOperationException();
    }
}
