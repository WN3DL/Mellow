package com.roxiun.mellow.feature.replay;

public class ReplayPlaybackState {

    private final boolean active;
    private final boolean paused;
    private final String replayId;
    private final String map;
    private final String mode;
    private final int currentTimeMs;
    private final int durationMs;
    private final double speed;

    public ReplayPlaybackState(
        boolean active,
        boolean paused,
        String replayId,
        String map,
        String mode,
        int currentTimeMs,
        int durationMs,
        double speed
    ) {
        this.active = active;
        this.paused = paused;
        this.replayId = replayId;
        this.map = map;
        this.mode = mode;
        this.currentTimeMs = currentTimeMs;
        this.durationMs = durationMs;
        this.speed = speed;
    }

    public static ReplayPlaybackState inactive() {
        return new ReplayPlaybackState(false, true, "", "", "", 0, 0, 1.0D);
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPaused() {
        return paused;
    }

    public String getReplayId() {
        return replayId;
    }

    public String getMap() {
        return map;
    }

    public String getMode() {
        return mode;
    }

    public int getCurrentTimeMs() {
        return currentTimeMs;
    }

    public int getDurationMs() {
        return durationMs;
    }

    public double getSpeed() {
        return speed;
    }
}
