package com.roxiun.mellow.feature.profileviewer.model;

import java.util.Collections;
import java.util.Map;

public class PvSourceData {

    private final Map<String, Integer> bedwarsStats;
    private final Map<String, String> socialLinks;
    private final int bedwarsLevel;
    private final int networkLevel;
    private final int karma;
    private final int gifted;
    private final int achievementPoints;
    private final int tokens;
    private final int slumberTickets;
    private final int bedwarsExperience;
    private final boolean valid;

    public PvSourceData(
        Map<String, Integer> bedwarsStats,
        Map<String, String> socialLinks,
        int bedwarsLevel,
        int networkLevel,
        int karma,
        int gifted,
        int achievementPoints,
        int tokens,
        int slumberTickets,
        int bedwarsExperience,
        boolean valid
    ) {
        this.bedwarsStats = bedwarsStats == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(bedwarsStats);
        this.socialLinks = socialLinks == null
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(socialLinks);
        this.bedwarsLevel = Math.max(0, bedwarsLevel);
        this.networkLevel = Math.max(0, networkLevel);
        this.karma = Math.max(0, karma);
        this.gifted = Math.max(0, gifted);
        this.achievementPoints = Math.max(0, achievementPoints);
        this.tokens = Math.max(0, tokens);
        this.slumberTickets = Math.max(0, slumberTickets);
        this.bedwarsExperience = Math.max(0, bedwarsExperience);
        this.valid = valid;
    }

    public static PvSourceData empty() {
        return new PvSourceData(
            Collections.<String, Integer>emptyMap(),
            Collections.<String, String>emptyMap(),
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            false
        );
    }

    public int bedwarsStat(String key) {
        Integer value = bedwarsStats.get(key);
        return value == null ? 0 : value;
    }

    public String social(String key) {
        String value = socialLinks.get(key);
        return value == null ? "" : value;
    }

    public Map<String, String> getSocialLinks() {
        return socialLinks;
    }

    public int getBedwarsLevel() {
        return bedwarsLevel;
    }

    public int getNetworkLevel() {
        return networkLevel;
    }

    public int getKarma() {
        return karma;
    }

    public int getGifted() {
        return gifted;
    }

    public int getAchievementPoints() {
        return achievementPoints;
    }

    public int getTokens() {
        return tokens;
    }

    public int getSlumberTickets() {
        return slumberTickets;
    }

    public int getBedwarsExperience() {
        return bedwarsExperience;
    }

    public boolean isValid() {
        return valid;
    }
}
