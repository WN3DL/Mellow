package com.roxiun.mellow.api.seraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum SeraphBlacklistReportType {
    CHEATING_CLOSET("cc", "cheating_closet", "closet_cheating"),
    CHEATING_BLATANT("bc", "cheating_blatant", "blatant_cheating"),
    SNIPING("s", "sniping"),
    SNIPING_POTENTIAL("ps", "sniping_potential", "potential_sniper"),
    SNIPER_LEGIT("ls", "sniper_legit", "legit_sniping"),
    ALT("a", "alt"),
    BOT("bot", "bot"),
    CAUTION("c", "caution"),
    ANNOY_LIST("al", "annoy_list", "annoylist");

    private final String commandToken;
    private final String apiValue;
    private final String[] acceptedTokens;

    SeraphBlacklistReportType(
        String commandToken,
        String apiValue,
        String... alternateTokens
    ) {
        this.commandToken = commandToken;
        this.apiValue = apiValue;
        this.acceptedTokens = alternateTokens == null
            ? new String[0]
            : alternateTokens;
    }

    public String getCommandToken() {
        return commandToken;
    }

    public String getApiValue() {
        return apiValue;
    }

    public String getDisplayLabel() {
        return apiValue + " (" + commandToken + ")";
    }

    public static SeraphBlacklistReportType fromToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }

        String normalized = token.trim().toLowerCase(Locale.ROOT);
        for (SeraphBlacklistReportType type : values()) {
            if (
                normalized.equals(type.commandToken) ||
                normalized.equals(type.apiValue)
            ) {
                return type;
            }

            for (String acceptedToken : type.acceptedTokens) {
                if (normalized.equals(acceptedToken)) {
                    return type;
                }
            }
        }

        return null;
    }

    public static String getUsageOptions() {
        List<String> tokens = new ArrayList<>();
        for (SeraphBlacklistReportType type : values()) {
            tokens.add(type.commandToken);
        }
        return String.join("|", tokens);
    }

    public static String[] getCompletionTokens() {
        SeraphBlacklistReportType[] values = values();
        String[] tokens = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            tokens[i] = values[i].getCommandToken();
        }
        return tokens;
    }
}
