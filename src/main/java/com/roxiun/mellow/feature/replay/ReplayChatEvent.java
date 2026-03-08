package com.roxiun.mellow.feature.replay;

public class ReplayChatEvent {

    private int timestampMs;
    private String componentJson;
    private byte type;

    public ReplayChatEvent() {}

    public ReplayChatEvent(int timestampMs, String componentJson, byte type) {
        this.timestampMs = timestampMs;
        this.componentJson = componentJson;
        this.type = type;
    }

    public int getTimestampMs() {
        return timestampMs;
    }

    public String getComponentJson() {
        return componentJson;
    }

    public byte getType() {
        return type;
    }
}
