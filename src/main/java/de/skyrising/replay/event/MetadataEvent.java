package de.skyrising.replay.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.network.NetworkState;
import net.minecraft.util.PacketByteBuf;

import java.io.IOException;

public class MetadataEvent extends Event {
    private static final Gson GSON = new Gson();
    public JsonObject data;

    MetadataEvent(long timecode, NetworkState state) {
        super(timecode, state);
    }

    public MetadataEvent(long timecode, NetworkState state, JsonObject data) {
        super(timecode, state);
        this.data = data;
    }

    @Override
    public Type getType() {
        return Type.METADATA;
    }

    @Override
    public void read(PacketByteBuf buf) throws IOException {
        data = GSON.fromJson(buf.readString(), JsonObject.class);
    }

    @Override
    public void write(PacketByteBuf buf) throws IOException {
        buf.writeString(GSON.toJson(data));
    }

    @Override
    public String toString() {
        return "MetadataEvent{" + "data=" + data + ", timecode=" + timecode + '}';
    }
}
