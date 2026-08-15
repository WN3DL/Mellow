package com.roxiun.mellow.util.ping;

import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.gamestate.GameSnapshot;

public final class PingProviderUtils {

    public static final int PROVIDER_NONE = 0;
    public static final int PROVIDER_AURORA = 1;
    public static final int PROVIDER_LUNA = 2;
    public static final int PROVIDER_SERAPH = 3;

    private PingProviderUtils() {}

    public static boolean shouldUseAurora(MellowOneConfig config) {
        return (
            config != null &&
            config.pingProvider == PROVIDER_AURORA
        );
    }

    public static boolean shouldUseLuna(MellowOneConfig config) {
        return config != null && config.pingProvider == PROVIDER_LUNA;
    }

    public static boolean shouldUseSeraph(MellowOneConfig config) {
        return (
            config != null &&
            config.pingProvider == PROVIDER_SERAPH
        );
    }

    public static boolean hasLunaApiKey(MellowOneConfig config) {
        return hasValue(config == null ? null : config.lunaPingApiKey);
    }

    public static boolean hasSeraphApiKey(MellowOneConfig config) {
        return hasValue(config == null ? null : config.seraphKey);
    }

    public static boolean canUseExternalPing(GameSnapshot snapshot) {
        return snapshot != null && snapshot.isOnHypixel() && !snapshot.isLobby();
    }

    private static boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
