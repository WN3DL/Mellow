package com.roxiun.mellow.util.annoylist;

public class AnnoylistedPlayer {
    private final String name;
    private final String reason;

    public AnnoylistedPlayer(String name, String reason) {
        this.name = name;
        this.reason = reason;
    }

    public String getName() {
        return name;
    }

    public String getReason() {
        return reason;
    }
}
