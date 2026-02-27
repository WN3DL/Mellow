package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.feature.nicks.NickUtils;
import com.roxiun.mellow.feature.tags.TagUtils;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.hypixel.data.type.GameType;

public class StatsChecker {

    private final PlayerCache playerCache;
    private final NickUtils nickUtils;
    private final MellowOneConfig config;
    private final Map<String, TabStats> tabStats;
    private final TagUtils tagUtils;
    private final BlacklistManager blacklistManager;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<String> tabFetchInFlight = ConcurrentHashMap.newKeySet();

    public StatsChecker(
        PlayerCache playerCache,
        NickUtils nickUtils,
        MellowOneConfig config,
        Map<String, TabStats> tabStats,
        TagUtils tagUtils,
        BlacklistManager blacklistManager
    ) {
        this.playerCache = playerCache;
        this.nickUtils = nickUtils;
        this.config = config;
        this.tabStats = tabStats;
        this.tagUtils = tagUtils;
        this.blacklistManager = blacklistManager;
    }

    public void checkPlayerStats(List<String> onlinePlayers) {
        tabStats.clear();
        if (onlinePlayers == null || onlinePlayers.isEmpty()) {
            return;
        }

        final StatScope activeScope = resolveActiveScope();
        final int MAX_THREADS = 20;
        int poolSize = Math.min(onlinePlayers.size(), MAX_THREADS);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        for (String playerName : onlinePlayers) {
            if (nickUtils.isNicked(playerName)) continue;

            executor.submit(() -> {
                // Force a refresh by clearing the player from the cache first
                playerCache.clearPlayer(playerName);
                PlayerProfile profile = playerCache.getProfile(playerName);

                if (profile == null || !hasStatsForScope(profile, activeScope)) {
                    return;
                }

                if (!passesScopeFilters(profile, activeScope)) {
                    return;
                }

                // Populate TabStats for the tab list
                if (config.tabStats) {
                    TabStats newTabStats = profile.getTabStats(activeScope);
                    if (newTabStats != null) {
                        tabStats.put(playerName, newTabStats);
                    }
                }

                // Print stats to chat if enabled
                if (config.printStats) {
                    String chatMessage = formatChatStats(profile, activeScope);
                    if (!chatMessage.isEmpty()) {
                        mc.addScheduledTask(() ->
                            ChatUtils.sendMessage(chatMessage)
                        );
                    }
                }

                // Print Urchin tags to chat if enabled
                if (
                    config.urchin &&
                    config.printBlacklistTags &&
                    profile.isUrchinTagged()
                ) {
                    String tags = FormattingUtils.formatUrchinTags(
                        profile.getUrchinTags()
                    );
                    String urchinMessage =
                        "§c" +
                        profile.getName() +
                        " is tagged on §5Urchin§c for: " +
                        tags;
                    mc.addScheduledTask(() ->
                        ChatUtils.sendMessage(urchinMessage)
                    );
                }

                // Print Seraph tags to chat if enabled
                if (
                    config.seraph &&
                    config.printBlacklistTags &&
                    profile.isSeraphTagged()
                ) {
                    String formattedTags = FormattingUtils.formatSeraphTags(
                        profile.getSeraphTags()
                    );
                    // Split the formatted tags by the newline separator and send as separate messages
                    String[] tagMessages = formattedTags.split("\n§c");
                    if (
                        tagMessages.length > 0 &&
                        !tagMessages[0].trim().isEmpty()
                    ) {
                        // Send the first tag with the main message
                        String firstMessage =
                            "§c" +
                            profile.getName() +
                            " is tagged on §3Seraph§c for: " +
                            tagMessages[0];
                        mc.addScheduledTask(() ->
                            ChatUtils.sendMessage(firstMessage)
                        );
                        // Send additional tags as separate messages
                        for (int i = 1; i < tagMessages.length; i++) {
                            if (!tagMessages[i].trim().isEmpty()) {
                                String additionalMessage =
                                    "§c" + tagMessages[i];
                                mc.addScheduledTask(() ->
                                    ChatUtils.sendMessage(additionalMessage)
                                );
                            }
                        }
                    }
                }

                // Check if player is on blacklist and print a message if they are
                java.util.UUID uuid = UUIDUtils.fromString(profile.getUuid());
                if (blacklistManager.isBlacklisted(uuid)) {
                    mc.addScheduledTask(() -> {
                        ChatUtils.sendMessage(
                            "§c" +
                                profile.getName() +
                                " is on your blacklist"
                        );
                        // Play pling sound when blacklisted player is detected
                        mc.thePlayer.playSound("note.pling", 1.0F, 1.0F);
                    });
                }
            });
        }

        executor.shutdown();
        // The notification for completion can be added back if desired
    }

    public void fetchTabStatsForPlayers(
        List<String> playerNames,
        boolean clearBeforeFetch
    ) {
        if (clearBeforeFetch) {
            tabStats.clear();
        }
        if (!config.tabStats || playerNames == null || playerNames.isEmpty()) {
            return;
        }

        final StatScope activeScope = resolveActiveScope();
        for (String playerName : playerNames) {
            if (playerName == null || playerName.isEmpty()) {
                continue;
            }
            if (nickUtils.isNicked(playerName)) {
                continue;
            }

            String normalizedName = playerName.toLowerCase(Locale.ROOT);
            if (!tabFetchInFlight.add(normalizedName)) {
                continue;
            }

            AsyncExecutor.getInstance().profileIo(() -> {
                try {
                    playerCache.clearPlayer(playerName);
                    PlayerProfile profile = playerCache.getProfile(playerName);

                    if (profile == null || !hasStatsForScope(profile, activeScope)) {
                        return;
                    }
                    if (!passesScopeFilters(profile, activeScope)) {
                        return;
                    }

                    TabStats newTabStats = profile.getTabStats(activeScope);
                    if (newTabStats != null) {
                        tabStats.put(playerName, newTabStats);
                    }
                } finally {
                    tabFetchInFlight.remove(normalizedName);
                }
            });
        }
    }

    private StatScope resolveActiveScope() {
        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        if (snapshot != null && snapshot.getGameType() == GameType.SKYWARS) {
            return StatScope.SKYWARS;
        }
        return StatScope.BEDWARS;
    }

    private boolean hasStatsForScope(PlayerProfile profile, StatScope scope) {
        if (scope == StatScope.SKYWARS) {
            return profile.getSkywarsPlayer() != null;
        }
        return profile.getBedwarsPlayer() != null;
    }

    private boolean passesScopeFilters(PlayerProfile profile, StatScope scope) {
        if (scope == StatScope.SKYWARS) {
            return true;
        }

        BedwarsPlayer player = profile.getBedwarsPlayer();
        return player != null && player.getFkdr() >= config.minFkdr;
    }

    private String formatChatStats(PlayerProfile profile, StatScope scope) {
        if (scope == StatScope.SKYWARS) {
            return formatSkywarsChatStats(profile);
        }
        return formatBedwarsChatStats(profile);
    }

    private String formatBedwarsChatStats(PlayerProfile profile) {
        BedwarsPlayer player = profile.getBedwarsPlayer();
        if (player == null) {
            return "";
        }

        String displayName = player.getFormattedNameWithRank();
        String stars = player.getStars();
        String fkdr = player.getFkdrColor() + player.getFormattedFkdr();

        String winstreak = "";
        if (player.getWinstreak() > 0) {
            winstreak = FormattingUtils.formatWinstreak(
                String.valueOf(player.getWinstreak())
            );
        }

        String base = String.format(
            "%s §r%s§r§7 |§r FKDR: %s",
            displayName,
            stars,
            fkdr
        );

        if (config.tags) {
            String tagsValue = buildTagsValue(profile);
            if (winstreak.isEmpty()) {
                return String.format("%s §r§7|§r [ %s ]", base, tagsValue);
            } else {
                return String.format(
                    "%s §r§7|§r WS: %s§r [ %s ]",
                    base,
                    winstreak,
                    tagsValue
                );
            }
        } else {
            if (winstreak.isEmpty()) {
                return base;
            } else {
                return String.format("%s §r§7|§r WS: %s§r", base, winstreak);
            }
        }
    }

    private String formatSkywarsChatStats(PlayerProfile profile) {
        SkywarsPlayer player = profile.getSkywarsPlayer();
        if (player == null) {
            return "";
        }

        String base = String.format(
            "%s §r%s§r§7 |§r KDR: %s§r§7 |§r WLR: %s§r",
            player.getFormattedNameWithRank(),
            player.getLevelFormatted(),
            player.getFormattedKdrWithColor(),
            player.getFormattedWlrWithColor()
        );

        return base;
    }

    private String buildTagsValue(PlayerProfile profile) {
        BedwarsPlayer player = profile.getBedwarsPlayer();
        int starsInt = 0;
        try {
            starsInt = Integer.parseInt(
                player.getStars().replaceAll("§.", "").replaceAll("[^0-9]", "")
            );
        } catch (NumberFormatException ignored) {}

        String tagsValue = tagUtils.buildTags(
            profile.getName(),
            profile.getUuid(),
            starsInt,
            player.getFkdr(),
            player.getWinstreak(),
            player.getFinalKills(),
            player.getFinalDeaths()
        );

        if (tagsValue.endsWith(" ")) {
            return tagsValue.substring(0, tagsValue.length() - 1);
        }
        return tagsValue;
    }
}
