package com.roxiun.mellow.feature.replay;

import java.io.File;

public class ReplayCatalogEntry {

    private final File directory;
    private final ReplayMetadata metadata;

    public ReplayCatalogEntry(File directory, ReplayMetadata metadata) {
        this.directory = directory;
        this.metadata = metadata;
    }

    public File getDirectory() {
        return directory;
    }

    public ReplayMetadata getMetadata() {
        return metadata;
    }
}
