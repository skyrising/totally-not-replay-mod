package de.skyrising.replay.world;

import com.mojang.authlib.GameProfile;
import de.skyrising.replay.Utils;
import de.skyrising.replay.event.Event;
import de.skyrising.replay.event.PacketEvent;
import de.skyrising.replay.recording.Replay;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.NetworkState;
import net.minecraft.network.Packet;

import javax.annotation.Nullable;
import java.io.IOException;

public class ReplayNetworkHandler extends ClientPlayNetworkHandler {
    private static final String PLAYER_NAME = "ReplayPlayer";
    public ReplayNetworkHandler(MinecraftClient client, Screen screen, Replay replay) {
        super(client, screen, new ReplayConnection(replay), new GameProfile(PlayerEntity.getOfflinePlayerUuid(PLAYER_NAME), PLAYER_NAME));
    }

    private static class ReplayConnection extends ClientConnection {
        private Replay replay;
        private long startTime = System.currentTimeMillis();
        public ReplayConnection(Replay replay) {
            super(NetworkSide.CLIENTBOUND);
            this.replay = replay;
        }

        @Override
        public void tick() {
            super.tick();
            try {
                replay.readUntil(System.currentTimeMillis() - startTime, this::processEvent);
            } catch (IOException e) {
                Utils.onError(e);
            }
        }

        private void processEvent(Event e) {
            if (e instanceof PacketEvent) {
                PacketEvent pe = ((PacketEvent) e);
                if (pe.networkState != NetworkState.PLAY) {
                    System.out.println("Invalid network state " + pe);
                    return;
                }
                @SuppressWarnings("unchecked")
                Packet<ClientPlayNetworkHandler> packet = (Packet<ClientPlayNetworkHandler>) pe.getPacket();
                packet.apply((ClientPlayNetworkHandler) this.getPacketListener());
            }
        }

        @Override
        public void send(Packet<?> packet, @Nullable GenericFutureListener<? extends Future<? super Void>> listener) {}
    }
}
