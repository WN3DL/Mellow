package com.roxiun.mellow.feature.stats;

public enum InGameBlacklistWarningDestination {
    NONE,
    ALL_CHAT,
    PARTY_CHAT;

    public static InGameBlacklistWarningDestination fromConfig(int configValue) {
        if (configValue == 1) {
            return ALL_CHAT;
        }
        if (configValue == 2) {
            return PARTY_CHAT;
        }
        return NONE;
    }

    public String getCommandPrefix() {
        if (this == ALL_CHAT) {
            return "/ac";
        }
        if (this == PARTY_CHAT) {
            return "/pc";
        }
        return null;
    }
}
