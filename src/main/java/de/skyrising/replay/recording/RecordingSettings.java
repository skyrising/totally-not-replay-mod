package de.skyrising.replay.recording;

import net.minecraft.client.network.packet.*;
import net.minecraft.network.Packet;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class RecordingSettings {
    public PacketFilter filter = PacketFilter.USED;

    public Set<Class<? extends Packet<?>>> getIgnoredPackets() {
        switch (filter) {
            case ALL: return Collections.emptySet();
            case USED: {
                return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                    AdvancementUpdateS2CPacket.class,
                    CommandSuggestionsS2CPacket.class,
                    CommandTreeS2CPacket.class,
                    ConfirmGuiActionS2CPacket.class,
                    CooldownUpdateS2CPacket.class,
                    CraftFailedResponseS2CPacket.class,
                    ExperienceBarUpdateS2CPacket.class,
                    GuiCloseS2CPacket.class,
                    GuiOpenS2CPacket.class,
                    GuiUpdateS2CPacket.class,
                    KeepAliveS2CPacket.class,
                    OpenContainerPacket.class,
                    MapUpdateS2CPacket.class,
                    ScoreboardDisplayS2CPacket.class,
                    ScoreboardObjectiveUpdateS2CPacket.class,
                    ScoreboardPlayerUpdateS2CPacket.class,
                    SelectAdvancementTabS2CPacket.class,
                    SetCameraEntityS2CPacket.class,
                    SetTradeOffersPacket.class,
                    SignEditorOpenS2CPacket.class,
                    StatisticsS2CPacket.class,
                    SynchronizeRecipesS2CPacket.class,
                    SynchronizeTagsS2CPacket.class,
                    TagQueryResponseS2CPacket.class,
                    UnlockRecipesS2CPacket.class
                )));
            }
        }
        throw new UnsupportedOperationException();
    }
    enum PacketFilter {
        ALL, USED, CUSTOM
    }
}
