package de.skyrising.replay.mixin.accessors;

import net.minecraft.client.network.packet.ChunkDataS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkDataS2CPacket.class)
public interface ChunkDataS2CPacketAccessor {
    @Accessor
    int getChunkX();

    @Accessor
    int getChunkZ();
}
