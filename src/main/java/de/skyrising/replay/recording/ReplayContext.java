package de.skyrising.replay.recording;

import de.skyrising.replay.event.CompressionSettingEvent;
import de.skyrising.replay.event.Event;
import de.skyrising.replay.event.PacketEvent;
import net.minecraft.client.network.packet.LoginSuccessS2CPacket;
import net.minecraft.client.network.packet.QueryResponseS2CPacket;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;

abstract class ReplayContext {
    public static final int VERSION = 1;
    protected CompressionSettingEvent compressionSettings;
    protected NetworkState networkState = NetworkState.LOGIN;

    protected void onEvent(Event e) {
        if (e instanceof CompressionSettingEvent) {
            compressionSettings = (CompressionSettingEvent) e;
            onCompressionUpdate();
        } else if (e instanceof PacketEvent) {
            Packet<?> packet = ((PacketEvent) e).getPacket();
            if (packet instanceof LoginSuccessS2CPacket) {
                networkState = NetworkState.PLAY;
            } else if (packet instanceof QueryResponseS2CPacket) {
                networkState = NetworkState.STATUS;
            }
        }
    }

    protected abstract void onCompressionUpdate();
}
