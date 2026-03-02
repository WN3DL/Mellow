package com.roxiun.mellow.util.tagignore;

public class TagIgnoredPlayer {
    private final String name;
    private final String reason;

    public TagIgnoredPlayer(String name, String reason) {
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
