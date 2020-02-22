package de.skyrising.replay.event;

import de.skyrising.replay.Utils;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.util.PacketByteBuf;

import java.io.IOException;

public class PacketEvent extends Event {
    private Packet<?> packet;

    PacketEvent(long timecode, NetworkState state) {
        super(timecode, state);
    }

    public PacketEvent(long timecode, NetworkState state, Packet<?> packet) {
        super(timecode, state);
        this.packet = packet;
    }

    public Packet<?> getPacket() {
        return packet;
    }

    @Override
    public Type getType() {
        return Type.PACKET;
    }

    @Override
    public void read(PacketByteBuf buf) throws IOException {
        int id = buf.readVarInt();
        this.packet = networkState.getPacketHandler(NetworkSide.CLIENTBOUND, id);
        if (this.packet == null) throw new IOException("Invalid packet id " + id + " for state " + networkState);
        this.packet.read(buf);
    }

    @Override
    public void write(PacketByteBuf buf) throws IOException {
        Integer id = networkState.getPacketId(NetworkSide.CLIENTBOUND, packet);
        if (id == null) throw new IOException("Invalid packet " + packet.getClass() + " for state " + networkState);
        buf.writeVarInt(id);
        packet.write(buf);
    }

    @Override
    public String toString() {
        return "PacketEvent{" +
            "packet=" + Utils.toString(packet) +
            ", timecode=" + timecode +
            '}';
    }
}
