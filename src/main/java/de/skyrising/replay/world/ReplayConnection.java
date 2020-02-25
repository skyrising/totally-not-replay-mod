package de.skyrising.replay.world;

import de.skyrising.replay.Utils;
import de.skyrising.replay.event.Event;
import de.skyrising.replay.event.PacketEvent;
import de.skyrising.replay.recording.Replay;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;
import net.minecraft.network.listener.PacketListener;

import javax.annotation.Nullable;
import java.io.IOException;

public class ReplayConnection extends ClientConnection {
    private Replay replay;
    private long startTime = System.currentTimeMillis();
    private NetworkState state = NetworkState.LOGIN;

    public ReplayConnection(Replay replay, Screen parentGui) {
        super(NetworkSide.CLIENTBOUND);
        this.replay = replay;
        setPacketListener(new ClientLoginNetworkHandler(this, MinecraftClient.getInstance(), parentGui, text -> {}));
    }

    @Override
    public void setState(NetworkState networkState) {
        state = networkState;
    }

    @Override
    public void tick() {
        super.tick();
        if (replay == null) return;
        try {
            replay.readUntil(System.currentTimeMillis() - startTime, this::processEvent);
        } catch (IOException e) {
            Utils.onError(e);
            try {
                replay.close();
                replay = null;
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void processEvent(Event e) {
        if (e instanceof PacketEvent) {
            PacketEvent pe = ((PacketEvent) e);
            if (pe.networkState != state) {
                System.err.println("Invalid network state: " + pe);
                return;
            }
            @SuppressWarnings("unchecked")
            Packet<PacketListener> packet = (Packet<PacketListener>) pe.getPacket();
            packet.apply(this.getPacketListener());
        }
        System.out.println(e);
    }

    @Override
    public void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> listener) {}
}
