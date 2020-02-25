package de.skyrising.replay.world;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.client.network.packet.LoginSuccessS2CPacket;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkState;
import net.minecraft.text.Text;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class ReplayLoginHandler extends ClientLoginNetworkHandler {
    private final MinecraftClient client;
    private final Screen parentGui;
    public ReplayLoginHandler(ClientConnection connection, MinecraftClient client, @Nullable Screen parentGui, Consumer<Text> statusConsumer) {
        super(connection, client, parentGui, statusConsumer);
        this.client = client;
        this.parentGui = parentGui;
    }

    @Override
    public void onLoginSuccess(LoginSuccessS2CPacket loginSuccess) {
        ClientConnection connection = getConnection();
        connection.setState(NetworkState.PLAY);
        connection.setPacketListener(new ReplayNetworkHandler(client, parentGui, connection, loginSuccess.getProfile()));
    }
}
