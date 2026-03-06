package com.roxiun.mellow.feature.replay;

public class ReplayPacketFrame {

    private final int timestampMs;
    private final String className;
    private final byte[] payload;

    public ReplayPacketFrame(int timestampMs, String className, byte[] payload) {
        this.timestampMs = timestampMs;
        this.className = className;
        this.payload = payload;
    }

    public int getTimestampMs() {
        return timestampMs;
    }

    public String getClassName() {
        return className;
    }

    public byte[] getPayload() {
        return payload;
    }
}
