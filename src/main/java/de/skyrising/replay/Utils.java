package de.skyrising.replay;

import com.google.common.base.MoreObjects;
import de.skyrising.replay.gui.ReplayBrowserScreen;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.network.Packet;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.PacketByteBuf;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class Utils {
    private Utils() {}

    @SuppressWarnings("rawtypes")
    public static String toString(Packet packet) {
        try {
            Class<?> currentCls = packet.getClass();
            MoreObjects.ToStringHelper helper = MoreObjects.toStringHelper(currentCls);
            while (currentCls != Object.class) {
                Field[] fields = currentCls.getDeclaredFields();
                for (Field f : fields) {
                    if ((f.getModifiers() & Modifier.STATIC) != 0) continue;
                    f.setAccessible(true);
                    Class<?> type = f.getType();
                    if (type.isArray() && type.getComponentType().isPrimitive()) continue;
                    helper.add(f.getName(), f.get(packet));
                }
                currentCls = currentCls.getSuperclass();
            }
            return helper.toString();
        } catch (IllegalAccessException e) {
            return packet.toString();
        }
    }

    public static void writeZigZagVarInt(PacketByteBuf buf, int value) {
        buf.writeVarInt((value << 1) ^ (value >> 31));
    }

    public static int readZigZagVarInt(PacketByteBuf buf) {
        int enc = buf.readVarInt();
        return (enc >>> 1) ^ ((enc << 31) >> 31);
    }

    public static void onError(Exception e) {
        e.printStackTrace();
        disconnectFromReplay(new LiteralText(e.getMessage()));
    }

    public static void disconnectFromReplay(Text reason) {
        DisconnectedScreen screen = new DisconnectedScreen(new ReplayBrowserScreen(new TitleScreen()), "Stopped Replay", reason);
        MinecraftClient.getInstance().openScreen(screen);
        screen.children().forEach(child -> {
            if (child instanceof ButtonWidget) ((ButtonWidget) child).setMessage(I18n.translate("gui.back"));
        });
    }

    public static byte[] resize(byte[] arr, int size) {
        if (arr.length == size) return arr;
        if (arr.length > size) return Arrays.copyOfRange(arr, 0, size);
        byte[] newArr = new byte[size];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        return newArr;
    }

    public static String hexdump(ByteBuf buf) {
        int readerIndex = buf.readerIndex();
        int writerIndex = buf.writerIndex();
        byte[] line = new byte[16];
        int length = writerIndex - readerIndex;
        if (length == 0) return "empty";
        int digits = 0;
        for (int i = 0; i < 31; i++) {
            if (((length - 1) >> i) == 0) {
                digits = (i + 3) >> 2;
                break;
            }
        }
        int i = 0;
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%0" + digits + "x: ", i));
        for (; i < length; i++) {
            byte b = buf.getByte(readerIndex + i);
            if (i > 0 && (i & 0xf) == 0) {
                sb.append("| ");
                for (int j = 0; j < 16; j++) {
                    byte c = line[j];
                    sb.append(c < 0x20 || c >= 0x7f ? '.' : (char) c);
                }
                sb.append('\n');
                sb.append(String.format("%0" + digits + "x: ", i));
            }
            sb.append(String.format("%02x ", b & 0xff));
            line[i & 0xf] = b;
        }
        if (i > 0) {
            i = ((i - 1) & 0xf) + 1;
            for (int j = 16; j > i; j--) sb.append("   ");
            sb.append("| ");
            for (int j = 0; j < i; j++) {
                byte c = line[j];
                sb.append(c < 0x20 || c >= 0x7f ? '.' : (char) c);
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
