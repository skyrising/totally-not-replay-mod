package de.skyrising.replay.recording;

import de.skyrising.replay.Utils;

import java.io.File;
import java.io.IOException;

public class ReplaySummary {
    public final File file;
    public final long length;
    public final long duration;

    ReplaySummary(File file, long length, long duration) {
        this.file = file;
        this.length = length;
        this.duration = duration;
    }

    public static ReplaySummary read(File file) throws IOException {
        try (Replay replay = new Replay(file, "r")) {
            return replay.readSummary();
        }
    }

    public void load() {
        try {
            new Replay(file, "rw").load();
        } catch (IOException e) {
            Utils.onError(e);
        }
    }
}
