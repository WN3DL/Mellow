package com.roxiun.mellow.feature.replay;

import java.util.UUID;

public class ReplayMetadata {

    private String replayId;
    private String map;
    private String mode;
    private String serverName;
    private String gameType;
    private long startedAt;
    private long endedAt;
    private int durationMs;
    private int packetCount;
    private UUID viewerUuid;
    private String viewerName;
    private Integer recordedPlayerEntityId;
    private UUID recordedPlayerUuid;
    private String recordedPlayerName;
    private int formatVersion = 1;

    public String getReplayId() {
        return replayId;
    }

    public void setReplayId(String replayId) {
        this.replayId = replayId;
    }

    public String getMap() {
        return map == null ? "" : map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public String getMode() {
        return mode == null ? "" : mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getServerName() {
        return serverName == null ? "" : serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getGameType() {
        return gameType == null ? "" : gameType;
    }

    public void setGameType(String gameType) {
        this.gameType = gameType;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(long startedAt) {
        this.startedAt = startedAt;
    }

    public long getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(long endedAt) {
        this.endedAt = endedAt;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(int durationMs) {
        this.durationMs = durationMs;
    }

    public int getPacketCount() {
        return packetCount;
    }

    public void setPacketCount(int packetCount) {
        this.packetCount = packetCount;
    }

    public UUID getViewerUuid() {
        return viewerUuid;
    }

    public void setViewerUuid(UUID viewerUuid) {
        this.viewerUuid = viewerUuid;
    }

    public String getViewerName() {
        return viewerName == null ? "" : viewerName;
    }

    public void setViewerName(String viewerName) {
        this.viewerName = viewerName;
    }

    public Integer getRecordedPlayerEntityId() {
        return recordedPlayerEntityId;
    }

    public void setRecordedPlayerEntityId(Integer recordedPlayerEntityId) {
        this.recordedPlayerEntityId = recordedPlayerEntityId;
    }

    public UUID getRecordedPlayerUuid() {
        return recordedPlayerUuid;
    }

    public void setRecordedPlayerUuid(UUID recordedPlayerUuid) {
        this.recordedPlayerUuid = recordedPlayerUuid;
    }

    public String getRecordedPlayerName() {
        return recordedPlayerName == null ? "" : recordedPlayerName;
    }

    public void setRecordedPlayerName(String recordedPlayerName) {
        this.recordedPlayerName = recordedPlayerName;
    }

    public int getFormatVersion() {
        return formatVersion;
    }

    public void setFormatVersion(int formatVersion) {
        this.formatVersion = formatVersion;
    }
}
