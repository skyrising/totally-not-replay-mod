package de.skyrising.replay.world;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.LevelInfo;

public class ReplayWorld extends ClientWorld {
    public ReplayWorld(ClientPlayNetworkHandler clientPlayNetworkHandler, LevelInfo levelInfo, DimensionType dimensionType, int chunkLoadDistance, Profiler profiler, WorldRenderer worldRenderer) {
        super(clientPlayNetworkHandler, levelInfo, dimensionType, chunkLoadDistance, profiler, worldRenderer);
    }
}
