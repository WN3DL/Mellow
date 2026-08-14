package com.roxiun.mellow.api.provider.model;

public class PlayerIdentity {

    private final String username;
    private final String uuid;

    public PlayerIdentity(String username, String uuid) {
        this.username = username;
        this.uuid = uuid;
    }

    public String getUsername() {
        return username;
    }

    public String getUuid() {
        return uuid;
    }
}
