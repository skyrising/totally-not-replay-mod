package de.skyrising.replay.world;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;

public class ReplayNetworkHandler extends ClientPlayNetworkHandler {
    public ReplayNetworkHandler(MinecraftClient client, Screen screen, ClientConnection clientConnection, GameProfile gameProfile) {
        super(client, screen, clientConnection, gameProfile);
    }
}
