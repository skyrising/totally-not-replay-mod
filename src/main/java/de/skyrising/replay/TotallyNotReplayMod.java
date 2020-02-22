package de.skyrising.replay;

import net.fabricmc.api.ModInitializer;

import java.io.File;
import java.io.IOException;

public class TotallyNotReplayMod implements ModInitializer {
    @Override
    public void onInitialize() {

    }

    public static File getRecordingsFolder() throws IOException {
        File recordingsFolder = new File("recordings");
        if (!recordingsFolder.exists()) {
            if (!recordingsFolder.mkdirs()) throw new IOException("Could not create output directory");
        } else if (!recordingsFolder.isDirectory()) {
            throw new IOException("recordings/ is not a directory");
        }
        return recordingsFolder;
    }
}
