package de.skyrising.replay.event;

import net.minecraft.network.NetworkState;
import net.minecraft.util.PacketByteBuf;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public abstract class Event {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    public final long timecode;
    public final NetworkState networkState;

    protected Event(long timecode, NetworkState state) {
        this.timecode = timecode;
        this.networkState = state;
    }

    public abstract Type getType();

    public abstract void read(PacketByteBuf buf) throws IOException;
    public abstract void write(PacketByteBuf buf) throws IOException;

    public static Event read(NetworkState state, PacketByteBuf buf) throws IOException {
        long timecode = buf.readVarLong();
        int id = buf.readVarInt();
        if (id >= Type.values.length) throw new IOException("Invalid event type " + id);
        Type type = Type.values[id];
        Event e = type.instantiate(timecode, state);
        e.read(buf);
        return e;
    }

    public static void write(PacketByteBuf buf, Event e) throws IOException {
        buf.writeVarLong(e.timecode);
        buf.writeVarInt(e.getType().ordinal());
        e.write(buf);
    }

    public enum Type {
        COMPRESSION(CompressionSettingEvent.class),
        METADATA(MetadataEvent.class),
        PACKET(PacketEvent.class),
        SNAPSHOT(SnapshotEvent.class);

        static final Type[] values = values();

        public final Class<? extends Event> cls;
        private final MethodHandle constr;
        Type(Class<? extends Event> cls) {
            this.cls = cls;
            try {
                this.constr = LOOKUP.findConstructor(cls, MethodType.methodType(void.class, long.class, NetworkState.class));
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Default event constructor does not exist", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Default event constructor is not accessible", e);
            }
        }

        public Event instantiate(long timecode, NetworkState state) {
            try {
                return (Event) constr.invoke(timecode, state);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }
}
