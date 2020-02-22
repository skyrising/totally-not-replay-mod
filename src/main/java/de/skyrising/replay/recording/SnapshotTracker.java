package de.skyrising.replay.recording;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import de.skyrising.replay.Utils;
import de.skyrising.replay.event.Event;
import de.skyrising.replay.event.PacketEvent;
import de.skyrising.replay.mixin.accessors.ChunkDataS2CPacketAccessor;
import de.skyrising.replay.mixin.accessors.ChunkDeltaUpdateS2CPacketAccessor;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.client.network.packet.ChunkDataS2CPacket;
import net.minecraft.client.network.packet.ChunkDeltaUpdateS2CPacket;
import net.minecraft.network.Packet;
import net.minecraft.util.PacketByteBuf;
import net.minecraft.util.math.ChunkPos;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

public class SnapshotTracker {
    private long previousSnapshot = 0;
    private final Long2ObjectMap<LongCollection> chunkEvents = new Long2ObjectLinkedOpenHashMap<>();

    public void onEvent(Event e, long offset) {
        switch (e.getType()) {
            case PACKET: {
                recordPacket(((PacketEvent) e).getPacket(), offset);
                break;
            }
            case SNAPSHOT: {
                previousSnapshot = offset;
                chunkEvents.clear();
                break;
            }
        }
    }

    private void recordPacket(Packet<?> packet, long offset) {
        if (packet instanceof ChunkDataS2CPacket) {
            ChunkDataS2CPacketAccessor accessor = ((ChunkDataS2CPacketAccessor) packet);
            chunkEvents.put(ChunkPos.toLong(accessor.getChunkX(), accessor.getChunkZ()), new LongArrayList(new long[]{offset}));
        } else if (packet instanceof ChunkDeltaUpdateS2CPacket) {
            ChunkPos pos = ((ChunkDeltaUpdateS2CPacketAccessor) packet).getChunkPos();
            chunkEvents.get(pos.toLong()).add(offset);
        }
    }

    private void write(PacketByteBuf buf) {
        buf.writeVarLong(previousSnapshot);
        buf.writeVarInt(chunkEvents.size());
        for (Long2ObjectMap.Entry<LongCollection> ce : chunkEvents.long2ObjectEntrySet()) {
            long key = ce.getLongKey();
            Utils.writeZigZagVarInt(buf, ChunkPos.getPackedX(key));
            Utils.writeZigZagVarInt(buf, ChunkPos.getPackedZ(key));
            LongCollection value = ce.getValue();
            buf.writeVarInt(value.size());
            for (long v : value) buf.writeVarLong(v);
        }
    }

    @Override
    public String toString() {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        write(buf);
        byte[] arr = new byte[buf.writerIndex()];
        buf.getBytes(0, arr);
        buf.release();
        return "SnapshotTracker" + Arrays.toString(arr);
    }

    private static class Long2LongMultimap implements Multimap<Long, Long> {
        private final Long2ObjectMap<LongCollection> map;
        private final Supplier<LongCollection> collectionSupplier;

        @SuppressWarnings("unchecked")
        public Long2LongMultimap(Supplier<Long2ObjectMap<?>> mapSupplier, Supplier<LongCollection> collectionSupplier) {
            this.map = (Long2ObjectMap<LongCollection>) mapSupplier.get();
            this.collectionSupplier = collectionSupplier;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public boolean containsKey(@Nullable Object key) {
            return key != null && containsKey((long) (Long) key);
        }

        public boolean containsKey(long key) {
            return map.containsKey(key);
        }

        @Override
        public boolean containsValue(@Nullable Object value) {
            return value != null && containsValue((long) (Long) value);
        }

        public boolean containsValue(long value) {
            for (LongCollection list : map.values()) {
                if (list.contains(value)) return true;
            }
            return false;
        }

        @Override
        public boolean containsEntry(@Nullable Object key, @Nullable Object value) {
            return key != null && value != null && containsEntry((long) (Long) key, (long) (Long) value);
        }

        public boolean containsEntry(long key, long value) {
            LongCollection mappedValues = map.get(key);
            return mappedValues.contains(value);
        }

        @Override
        public boolean put(@Nullable Long key, @Nullable Long value) {
            return key != null && value != null && put((long) key, (long) value);
        }

        public boolean put(long key, long value) {
            return get(key).add(value);
        }

        @Override
        public boolean remove(@Nullable Object key, @Nullable Object value) {
            return key != null && value != null && remove((long) (Long) key, (long) (Long) value);
        }

        public boolean remove(long key, long value) {
            LongCollection values = get(key);
            return values != null && values.rem(value);
        }

        @Override
        public boolean putAll(@Nullable Long key, Iterable<? extends Long> values) {
            if (key == null) return false;
            LongCollection mappedValues = get(key);
            boolean added = false;
            for (Long value : values) {
                if (value == null) continue;
                added |= mappedValues.add(value);
            }
            return added;
        }

        public boolean putAll(long key, LongIterable values) {
            LongCollection mappedValues = get(key);
            boolean added = false;
            for (Long value : values) {
                added |= mappedValues.add(value);
            }
            return added;
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public boolean putAll(Multimap<? extends Long, ? extends Long> multimap) {
            boolean added = false;
            for (Map.Entry entry : multimap.entries()) {
                added |= putAll((Long) entry.getKey(), (Collection<Long>) entry.getValue());
            }
            return added;
        }

        @Override
        public LongCollection replaceValues(@Nullable Long key, Iterable<? extends Long> values) {
            if (key == null) return LongLists.EMPTY_LIST;
            LongCollection previous = get(key);
            LongCollection c = collectionSupplier.get();
            for (Long l : values) c.add(l);
            map.put(key, c);
            return previous == null ? LongLists.EMPTY_LIST : LongCollections.unmodifiable(previous);
        }

        @Override
        public LongCollection removeAll(@Nullable Object key) {
            return key == null ? null : map.remove((long) key);
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public LongCollection get(@Nullable Long key) {
            return key == null ? LongLists.EMPTY_LIST : get((long) key);
        }

        public LongCollection get(long key) {
            LongCollection values = map.get(key);
            if (values == null) {
                values = collectionSupplier.get();
                map.put(key, values);
            }
            return values;
        }

        @Override
        public LongSet keySet() {
            return map.keySet();
        }

        @Override
        public Multiset<Long> keys() {
            throw new UnsupportedOperationException();
        }

        @Override
        public LongCollection values() {
            throw new UnsupportedOperationException();
        }

        public LongSet valueSet() {
            LongSet set = new LongOpenHashSet();
            for (LongCollection values : map.values()) {
                set.addAll(values);
            }
            return LongSets.unmodifiable(set);
        }

        @Override
        public Collection<Map.Entry<Long, Long>> entries() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long2ObjectMap<Collection<Long>> asMap() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return map.toString();
        }
    }
}
