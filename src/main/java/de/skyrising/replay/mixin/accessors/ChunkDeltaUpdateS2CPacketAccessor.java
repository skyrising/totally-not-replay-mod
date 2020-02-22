package de.skyrising.replay.mixin.accessors;

import net.minecraft.client.network.packet.ChunkDeltaUpdateS2CPacket;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChunkDeltaUpdateS2CPacket.class)
public interface ChunkDeltaUpdateS2CPacketAccessor {
    @Accessor
    ChunkPos getChunkPos();
}
