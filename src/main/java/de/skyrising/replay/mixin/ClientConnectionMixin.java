package de.skyrising.replay.mixin;

import de.skyrising.replay.recording.Recording;
import de.skyrising.replay.recording.RecordingSettings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin extends SimpleChannelInboundHandler<Packet<?>>  {
    @Shadow @Final private NetworkSide side;

    @Inject(method = "channelRead0", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;handlePacket(Lnet/minecraft/network/Packet;Lnet/minecraft/network/listener/PacketListener;)V"))
    private void onPacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo ci) {
        if (side == NetworkSide.CLIENTBOUND) {
            try {
                Recording.onClientPacket(packet);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "channelActive", at = @At("HEAD"))
    private void onActive(ChannelHandlerContext channelHandlerContext, CallbackInfo ci) {
        if (!Recording.shouldRecord((ClientConnection) (Object) this)) return;
        Recording.clientRecording = new Recording(new RecordingSettings());
        try {
            Recording.clientRecording.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "channelInactive", at = @At("HEAD"))
    private void onInactive(ChannelHandlerContext channelHandlerContext, CallbackInfo ci) {
        if (Recording.clientRecording != null && Recording.clientRecording.isActive()) {
            try {
                Recording.clientRecording.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Recording.clientRecording = null;
        }
    }
}
