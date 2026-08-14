package com.roxiun.mellow.feature.replay;

import java.io.File;
import java.util.List;

public class ReplayLoadedData {

    private final File directory;
    private final ReplayMetadata metadata;
    private final List<ReplayPacketFrame> packets;
    private final List<ReplayChatEvent> chats;
    private final List<ReplayScoreboardFrame> scoreboards;
    private final List<ReplayLocalPlayerSnapshot> localSnapshots;

    public ReplayLoadedData(
        File directory,
        ReplayMetadata metadata,
        List<ReplayPacketFrame> packets,
        List<ReplayChatEvent> chats,
        List<ReplayScoreboardFrame> scoreboards,
        List<ReplayLocalPlayerSnapshot> localSnapshots
    ) {
        this.directory = directory;
        this.metadata = metadata;
        this.packets = packets;
        this.chats = chats;
        this.scoreboards = scoreboards;
        this.localSnapshots = localSnapshots;
    }

    public File getDirectory() {
        return directory;
    }

    public ReplayMetadata getMetadata() {
        return metadata;
    }

    public List<ReplayPacketFrame> getPackets() {
        return packets;
    }

    public List<ReplayChatEvent> getChats() {
        return chats;
    }

    public List<ReplayScoreboardFrame> getScoreboards() {
        return scoreboards;
    }

    public List<ReplayLocalPlayerSnapshot> getLocalSnapshots() {
        return localSnapshots;
    }
}
