package de.skyrising.replay.gui;

import de.skyrising.replay.TotallyNotReplayMod;
import de.skyrising.replay.recording.Replay;
import de.skyrising.replay.recording.ReplaySummary;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReplayListWidget extends AlwaysSelectedEntryListWidget<ReplayListWidget.Entry> {
    private List<ReplaySummary> replays;
    public ReplayListWidget(ReplayBrowserScreen parent, MinecraftClient client, int width, int height, int top, int bottom, int itemHeight, @Nullable ReplayListWidget previous) {
        super(client, width, height, top, bottom, itemHeight);
        if (previous != null) replays = previous.replays;
        loadReplays(false);
    }

    public void loadReplays(boolean load) {
        this.clearEntries();
        if (replays == null || load) {
            try {
                File recordingsFolder = TotallyNotReplayMod.getRecordingsFolder();
                File[] files = recordingsFolder.listFiles((d, f) -> f.endsWith(".tnr"));
                if (files == null) return;
                replays = new ArrayList<>();
                for (File f : files) {
                    try {
                        replays.add(ReplaySummary.read(f));
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (ReplaySummary replay : replays) {
            addEntry(new Entry(this, replay));
        }
    }

    public Optional<ReplaySummary> getSelectedReplay() {
        return Optional.ofNullable(getSelected()).map(e -> e.summary);
    }

    public final class Entry extends AlwaysSelectedEntryListWidget.Entry<ReplayListWidget.Entry> implements AutoCloseable {
        private final MinecraftClient client;
        private final ReplayListWidget parent;
        public final ReplaySummary summary;

        public Entry(ReplayListWidget parent, ReplaySummary summary) {
            this.parent = parent;
            this.summary = summary;
            this.client = MinecraftClient.getInstance();
        }

        @Override
        public void close() {
        }

        @Override
        public void render(int index, int y, int x, int width, int height, int mouseX, int mouseY, boolean hovering, float delta) {
            String filename = summary.file.getName();
            int extIndex = filename.lastIndexOf(".tnr");
            if (extIndex > 0) filename = filename.substring(0, extIndex);
            this.client.textRenderer.draw(filename, x + 32 + 3, y + 1, 0xffffff);
            DrawableHelper.fill(x, y, x + 32, y + 32, 0xa0909090);
            String fileSize = String.format("%.2fMB", summary.length / (1024.0 * 1024.0));
            this.client.textRenderer.draw(fileSize, x + 32 + 3, y + 3 + 9, 0x808080);
            long time = summary.duration / 1000;
            int seconds = (int) (time % 60);
            time = (time - seconds) / 60;
            int minutes = (int) (time % 60);
            int hours = (int) ((time - minutes) / 60);
            String timeStr = hours > 0 ? String.format("%d:%02d:%02d", hours, minutes, seconds) : String.format("%d:%02d", minutes, seconds);
            this.client.textRenderer.draw(timeStr, x + 32 + 3, y + 3 + 9 + 9, 0x808080);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            parent.setSelected(this);
            return false;
        }
    }
}
