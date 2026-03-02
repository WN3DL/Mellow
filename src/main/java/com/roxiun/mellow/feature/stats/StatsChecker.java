package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.buildbattle.BuildBattlePlayer;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.tnt.TntRunPlayer;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.feature.alerts.AlertSoundGate;
import com.roxiun.mellow.feature.nicks.NickUtils;
import com.roxiun.mellow.feature.tags.TagUtils;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.annoylist.AnnoylistManager;
import com.roxiun.mellow.util.annoylist.AnnoylistedPlayer;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.tagignore.TagIgnoreManager;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;

public class StatsChecker {

    private final PlayerCache playerCache;
    private final NickUtils nickUtils;
    private final MellowOneConfig config;
    private final Map<String, TabStats> tabStats;
    private final TagUtils tagUtils;
    private final BlacklistManager blacklistManager;
    private final AnnoylistManager annoylistManager;
    private final TagIgnoreManager tagIgnoreManager;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final Set<String> tabFetchInFlight = ConcurrentHashMap.newKeySet();
    private final AlertSoundGate inGameAlertSoundGate = new AlertSoundGate();

    public StatsChecker(
        PlayerCache playerCache,
        NickUtils nickUtils,
        MellowOneConfig config,
        Map<String, TabStats> tabStats,
        TagUtils tagUtils,
        BlacklistManager blacklistManager,
        AnnoylistManager annoylistManager,
        TagIgnoreManager tagIgnoreManager
    ) {
        this.playerCache = playerCache;
        this.nickUtils = nickUtils;
        this.config = config;
        this.tabStats = tabStats;
        this.tagUtils = tagUtils;
        this.blacklistManager = blacklistManager;
        this.annoylistManager = annoylistManager;
        this.tagIgnoreManager = tagIgnoreManager;
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

                sendBlacklistAndTagAlerts(profile);
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

                    sendBlacklistAndTagAlerts(profile);
                } finally {
                    tabFetchInFlight.remove(normalizedName);
                }
            });
        }
    }

    public void resetInGameAlertSoundGate() {
        inGameAlertSoundGate.reset();
    }

    private StatScope resolveActiveScope() {
        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        return StatScopeResolver.resolveInGameScope(snapshot);
    }

    private boolean hasStatsForScope(PlayerProfile profile, StatScope scope) {
        if (scope == StatScope.SKYWARS) {
            return profile.getSkywarsPlayer() != null;
        }
        if (scope == StatScope.DUELS) {
            return profile.getDuelsPlayer() != null;
        }
        if (scope == StatScope.BUILD_BATTLE) {
            return profile.getBuildBattlePlayer() != null;
        }
        if (scope == StatScope.TNT_RUN) {
            return profile.getTntRunPlayer() != null;
        }
        return profile.getBedwarsPlayer() != null;
    }

    private boolean passesScopeFilters(PlayerProfile profile, StatScope scope) {
        if (
            scope == StatScope.SKYWARS ||
            scope == StatScope.DUELS ||
            scope == StatScope.BUILD_BATTLE ||
            scope == StatScope.TNT_RUN
        ) {
            return true;
        }

        BedwarsPlayer player = profile.getBedwarsPlayer();
        return player != null && player.getFkdr() >= config.minFkdr;
    }

    private String formatChatStats(PlayerProfile profile, StatScope scope) {
        if (scope == StatScope.SKYWARS) {
            return formatSkywarsChatStats(profile);
        }
        if (scope == StatScope.DUELS) {
            return formatDuelsChatStats(profile);
        }
        if (scope == StatScope.BUILD_BATTLE) {
            return formatBuildBattleChatStats(profile);
        }
        if (scope == StatScope.TNT_RUN) {
            return formatTntRunChatStats(profile);
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
        if (!player.hasWinstreakData()) {
            winstreak = "§7?";
        } else if (player.getWinstreak() > 0) {
            winstreak =
                FormattingUtils.formatWinstreak(
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

    private String formatDuelsChatStats(PlayerProfile profile) {
        DuelsPlayer player = profile.getDuelsPlayer();
        if (player == null) {
            return "";
        }

        String modeSuffix = player.getMode() == null || player.getMode().isOverall()
            ? " §7(Overall)"
            : " §7(" + player.getMode().getDisplayName() + ")";

        return String.format(
            "%s §r%s§r%s§7 |§r KDR: %s§r§7 |§r WLR: %s§r§7 |§r WS: %s§r",
            player.getFormattedNameWithRank(),
            player.getDivision(),
            modeSuffix,
            player.getFormattedKdrWithColor(),
            player.getFormattedWlrWithColor(),
            player.getFormattedWinstreakWithColor()
        );
    }

    private String formatBuildBattleChatStats(PlayerProfile profile) {
        BuildBattlePlayer player = profile.getBuildBattlePlayer();
        if (player == null) {
            return "";
        }

        return String.format(
            "%s §r%s§r§7 |§r WINS: %s§r",
            player.getFormattedNameWithRank(),
            player.getFormattedTitle(),
            player.getFormattedWinsWithColor()
        );
    }

    private String formatTntRunChatStats(PlayerProfile profile) {
        TntRunPlayer player = profile.getTntRunPlayer();
        if (player == null) {
            return "";
        }

        return String.format(
            "%s §r§7|§r WINS: %s§r§7 |§r RATIO: %s§r",
            player.getFormattedNameWithRank(),
            player.getFormattedWinsWithColor(),
            player.getFormattedRatioWithColor()
        );
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

    private void sendBlacklistAndTagAlerts(PlayerProfile profile) {
        if (profile == null) {
            return;
        }

        java.util.UUID uuid = UUIDUtils.fromString(profile.getUuid());
        boolean tagsIgnored =
            tagIgnoreManager != null && tagIgnoreManager.isTagIgnored(uuid);

        boolean urchinTagged =
            config.urchin &&
            config.printBlacklistTags &&
            profile.isUrchinTagged();
        boolean shouldPrintUrchinTagAlert = urchinTagged && !tagsIgnored;
        if (shouldPrintUrchinTagAlert) {
            String tags = FormattingUtils.formatUrchinTags(profile.getUrchinTags());
            String urchinMessage =
                "§c" + profile.getName() + " is tagged on §5Urchin§c for: " + tags;
            mc.addScheduledTask(() -> ChatUtils.sendMessage(urchinMessage));
        }

        boolean seraphTagged =
            config.seraph &&
            config.printBlacklistTags &&
            profile.isSeraphTagged();
        boolean shouldPrintSeraphTagAlert = seraphTagged && !tagsIgnored;
        if (shouldPrintSeraphTagAlert) {
            String formattedTags = FormattingUtils.formatSeraphTags(
                profile.getSeraphTags()
            );
            String[] tagMessages = formattedTags.split("\n§c");
            if (tagMessages.length > 0 && !tagMessages[0].trim().isEmpty()) {
                String firstMessage =
                    "§c" +
                    profile.getName() +
                    " is tagged on §3Seraph§c for: " +
                    tagMessages[0];
                mc.addScheduledTask(() -> ChatUtils.sendMessage(firstMessage));
                for (int i = 1; i < tagMessages.length; i++) {
                    if (!tagMessages[i].trim().isEmpty()) {
                        String additionalMessage = "§c" + tagMessages[i];
                        mc.addScheduledTask(() ->
                            ChatUtils.sendMessage(additionalMessage)
                        );
                    }
                }
            }
        }

        boolean blacklisted = blacklistManager.isBlacklisted(uuid);
        boolean annoylisted =
            annoylistManager != null && annoylistManager.isAnnoylisted(uuid);

        if (blacklisted || annoylisted) {
            BlacklistedPlayer blacklistedPlayer = blacklisted
                ? blacklistManager.getBlacklistedPlayer(uuid)
                : null;
            AnnoylistedPlayer annoylistedPlayer = annoylisted
                ? annoylistManager.getAnnoylistedPlayer(uuid)
                : null;
            String blacklistReasonSuffix = formatBlacklistReasonSuffix(
                blacklistedPlayer == null ? null : blacklistedPlayer.getReason()
            );
            String annoyReason = normalizeReason(
                annoylistedPlayer == null ? null : annoylistedPlayer.getReason()
            );

            mc.addScheduledTask(() -> {
                if (blacklisted) {
                    ChatUtils.sendMessage(
                        "§6" +
                        profile.getName() +
                        " §cis on your blacklist" +
                        blacklistReasonSuffix
                    );
                }
                if (annoylisted) {
                    ChatUtils.sendMessage(
                        "§6" +
                        profile.getName() +
                        " §3is on your annoy list: " +
                        annoyReason
                    );
                }
            });
        }

        if (
            blacklisted ||
            annoylisted ||
            shouldPrintUrchinTagAlert ||
            shouldPrintSeraphTagAlert
        ) {
            mc.addScheduledTask(() ->
                inGameAlertSoundGate.tryPlayPling(mc, 1.0F, 1.0F)
            );
        }
    }

    private String formatBlacklistReasonSuffix(String reason) {
        if (reason == null) {
            return "";
        }
        String trimmed = reason.trim();
        if (
            trimmed.isEmpty() ||
            BlacklistManager.isExternalFileImportReason(trimmed)
        ) {
            return "";
        }
        return ": " + trimmed;
    }

    private String normalizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "(none)";
        }
        return reason;
    }
}
