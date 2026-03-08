package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.buildbattle.BuildBattlePlayer;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.tnt.TntRunPlayer;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.cache.ProfileFetchContext;
import com.roxiun.mellow.cache.ProfileFetchResult;
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
import com.roxiun.mellow.util.player.PlayerUtils;
import com.roxiun.mellow.util.tagignore.TagIgnoreManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.ScorePlayerTeam;

public class StatsChecker {

    private static final String MC_COLOR_CODES = "0123456789abcdef";
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
    private final Set<String> reportedTabFetchFailuresThisMatch =
        ConcurrentHashMap.newKeySet();
    private final Set<UUID> outboundWarnedOpponentsThisMatch =
        ConcurrentHashMap.newKeySet();
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
            if (
                nickUtils.isNicked(playerName) ||
                PlayerUtils.isNickedOrNpc(playerName)
            ) continue;

            executor.submit(() -> {
                // Force a refresh by clearing the player from the cache first
                playerCache.clearPlayer(playerName);
                PlayerProfile profile = playerCache.getProfile(playerName);

                if (profile == null || !hasStatsForScope(profile, activeScope)) {
                    return;
                }

                boolean passesFilters = passesScopeFilters(profile, activeScope);

                // Populate TabStats for the tab list
                if (config.tabStats && passesFilters) {
                    TabStats newTabStats = profile.getTabStats(activeScope);
                    if (newTabStats != null) {
                        tabStats.put(playerName, newTabStats);
                    }
                }

                // Print stats to chat if enabled
                if (config.printStats && passesFilters) {
                    String chatMessage = formatChatStats(profile, activeScope);
                    if (!chatMessage.isEmpty()) {
                        mc.addScheduledTask(() ->
                            ChatUtils.sendMessage(chatMessage)
                        );
                    }
                }

                sendBlacklistAndTagAlerts(profile, playerName);
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
        if (playerNames == null || playerNames.isEmpty()) {
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
            if (PlayerUtils.isNickedOrNpc(playerName)) {
                continue;
            }

            String normalizedName = playerName.toLowerCase(Locale.ROOT);
            if (!tabFetchInFlight.add(normalizedName)) {
                continue;
            }

            AsyncExecutor.getInstance().profileIo(() -> {
                try {
                    ProfileFetchResult result = playerCache.getScopedProfileResult(
                        playerName,
                        activeScope,
                        ProfileFetchContext.LIVE_MATCH,
                        false
                    );
                    PlayerProfile profile = result.getProfile();

                    if (profile == null || !hasStatsForScope(profile, activeScope)) {
                        maybeReportLiveFetchFailure(playerName, result);
                        return;
                    }
                    boolean passesFilters = passesScopeFilters(
                        profile,
                        activeScope
                    );
                    boolean shouldPopulateTabStats = config.tabStats && passesFilters;

                    if (shouldPopulateTabStats) {
                        TabStats newTabStats = profile.getTabStats(activeScope);
                        if (newTabStats != null) {
                            tabStats.put(playerName, newTabStats);
                        }
                    }

                    if (shouldDeferRemoteTagLookup()) {
                        PlayerProfile baseProfile = profile;
                        AsyncExecutor.getInstance().profileIo(() -> {
                            PlayerProfile enrichedProfile =
                                playerCache.enrichProfileWithTags(baseProfile);

                            if (shouldPopulateTabStats) {
                                TabStats enrichedTabStats =
                                    enrichedProfile.getTabStats(activeScope);
                                if (enrichedTabStats != null) {
                                    tabStats.put(playerName, enrichedTabStats);
                                }
                            }

                            if (shouldScanForInGameWarnings()) {
                                sendBlacklistAndTagAlerts(
                                    enrichedProfile,
                                    playerName
                                );
                            }
                        });
                    } else if (shouldScanForInGameWarnings()) {
                        sendBlacklistAndTagAlerts(profile, playerName);
                    }
                } finally {
                    tabFetchInFlight.remove(normalizedName);
                }
            });
        }
    }

    private boolean shouldDeferRemoteTagLookup() {
        if (config == null) {
            return false;
        }

        boolean tabNeedsUrchinTags = config.showUrchinTagsInTab && config.urchin;
        boolean tabNeedsSeraphTags = config.showSeraphTagsInTab && config.seraph;
        boolean warningNeedsTags =
            config.printBlacklistTags && (config.urchin || config.seraph);
        return tabNeedsUrchinTags || tabNeedsSeraphTags || warningNeedsTags;
    }

    private void maybeReportLiveFetchFailure(
        String playerName,
        ProfileFetchResult result
    ) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }

        String reasonKey = result == null || result.getFailureReason() == null
            ? "UNKNOWN"
            : result.getFailureReason().name();
        String failureKey =
            playerName.toLowerCase(Locale.ROOT) + ":" + reasonKey;
        if (!reportedTabFetchFailuresThisMatch.add(failureKey)) {
            return;
        }

        String reason = StatsFetchFailureFormatter.describe(result);
        mc.addScheduledTask(() ->
            ChatUtils.sendMessage(
                "§cFailed to fetch stats for: §r" +
                playerName +
                "§c (" +
                reason +
                ")"
            )
        );
    }

    public void resetInGameAlertSoundGate() {
        inGameAlertSoundGate.reset();
    }

    public void resetInGameMatchWarningState() {
        inGameAlertSoundGate.reset();
        reportedTabFetchFailuresThisMatch.clear();
        outboundWarnedOpponentsThisMatch.clear();
    }

    public boolean shouldScanForInGameWarnings() {
        if (config == null) {
            return false;
        }
        if (config.inGameBlacklistWarningDestination != 0) {
            return true;
        }
        if (!blacklistManager.getBlacklist().isEmpty()) {
            return true;
        }
        if (
            annoylistManager != null && !annoylistManager.getAnnoylist().isEmpty()
        ) {
            return true;
        }
        return config.printBlacklistTags && (config.urchin || config.seraph);
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

    private void sendBlacklistAndTagAlerts(
        PlayerProfile profile,
        String tabPlayerName
    ) {
        if (profile == null) {
            return;
        }

        UUID uuid = UUIDUtils.fromString(profile.getUuid());
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

        boolean blacklisted = blacklistManager.isBlacklisted(uuid);
        boolean annoylisted =
            annoylistManager != null && annoylistManager.isAnnoylisted(uuid);
        BlacklistedPlayer blacklistedPlayer = blacklisted
            ? blacklistManager.getBlacklistedPlayer(uuid)
            : null;
        AnnoylistedPlayer annoylistedPlayer = annoylisted
            ? annoylistManager.getAnnoylistedPlayer(uuid)
            : null;

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

        if (blacklisted || annoylisted) {
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
            shouldSendOutboundOpponentWarning(
                uuid,
                tabPlayerName,
                blacklisted,
                shouldPrintUrchinTagAlert,
                shouldPrintSeraphTagAlert
            )
        ) {
            sendOutboundOpponentWarning(
                profile,
                tabPlayerName,
                blacklistedPlayer,
                blacklisted,
                shouldPrintUrchinTagAlert,
                shouldPrintSeraphTagAlert
            );
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

    private boolean shouldSendOutboundOpponentWarning(
        UUID uuid,
        String tabPlayerName,
        boolean blacklisted,
        boolean urchinTagged,
        boolean seraphTagged
    ) {
        if (uuid == null) {
            return false;
        }
        if (!blacklisted && !urchinTagged && !seraphTagged) {
            return false;
        }
        if (!isInBedwarsMatch()) {
            return false;
        }
        InGameBlacklistWarningDestination destination = resolveWarningDestination();
        if (destination == InGameBlacklistWarningDestination.NONE) {
            return false;
        }
        if (
            destination == InGameBlacklistWarningDestination.ALL_CHAT &&
            isBedwarsSolosMode()
        ) {
            return false;
        }
        if (!isOpponentByTabName(tabPlayerName)) {
            return false;
        }
        return outboundWarnedOpponentsThisMatch.add(uuid);
    }

    private void sendOutboundOpponentWarning(
        PlayerProfile profile,
        String tabPlayerName,
        BlacklistedPlayer blacklistedPlayer,
        boolean blacklisted,
        boolean urchinTagged,
        boolean seraphTagged
    ) {
        InGameBlacklistWarningDestination destination = resolveWarningDestination();
        String commandPrefix = destination.getCommandPrefix();
        if (commandPrefix == null) {
            return;
        }

        String playerName = profile.getName();
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Unknown";
        }
        String opponentTeamName = resolveOpponentTeamName(tabPlayerName, playerName);

        List<String> sourceLabels = new ArrayList<>(3);
        if (blacklisted) {
            sourceLabels.add("Local");
        }
        if (urchinTagged) {
            sourceLabels.add("Urchin");
        }
        if (seraphTagged) {
            sourceLabels.add("Seraph");
        }

        List<String> detailParts = new ArrayList<>(3);
        if (blacklisted) {
            detailParts.add(
                "Local: " + formatOutboundBlacklistReason(blacklistedPlayer)
            );
        }
        if (urchinTagged) {
            detailParts.add(
                "Urchin: " +
                normalizeOutboundDetail(
                    FormattingUtils.formatUrchinTags(profile.getUrchinTags())
                )
            );
        }
        if (seraphTagged) {
            detailParts.add(
                "Seraph: " +
                normalizeOutboundDetail(
                    FormattingUtils.formatSeraphTags(profile.getSeraphTags())
                )
            );
        }

        String mainMessage = opponentTeamName.isEmpty()
            ? "[Mellow] Flagged opponent: " +
            playerName +
            " [" +
            String.join(", ", sourceLabels) +
            "]"
            : "[Mellow] Flagged opponent on " +
            opponentTeamName +
            " team: " +
            playerName +
            " [" +
            String.join(", ", sourceLabels) +
            "]";
        String detailMessage = detailParts.isEmpty()
            ? null
            : "[Mellow] " + playerName + " tagged for: " + String.join(" | ", detailParts);

        mc.addScheduledTask(() -> {
            ChatUtils.sendChatCommandMessage(
                commandPrefix,
                mainMessage
            );
            if (detailMessage != null) {
                ChatUtils.sendChatCommandMessage(commandPrefix, detailMessage);
            }
        });
    }

    private InGameBlacklistWarningDestination resolveWarningDestination() {
        if (config == null) {
            return InGameBlacklistWarningDestination.NONE;
        }
        return InGameBlacklistWarningDestination.fromConfig(
            config.inGameBlacklistWarningDestination
        );
    }

    private boolean isInBedwarsMatch() {
        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        return snapshot != null && snapshot.isInBedwarsMatch();
    }

    private boolean isBedwarsSolosMode() {
        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        if (snapshot == null || !snapshot.isInBedwarsMatch()) {
            return false;
        }

        String mode = snapshot.getMode();
        if (mode == null || mode.trim().isEmpty()) {
            return false;
        }

        String normalized = mode
            .toLowerCase(Locale.ROOT)
            .replace('-', '_')
            .replaceAll("\\s+", "_");
        return normalized.contains("eight_one");
    }

    private boolean isOpponentByTabName(String tabPlayerName) {
        if (mc == null || mc.thePlayer == null) {
            return false;
        }
        if (tabPlayerName == null || tabPlayerName.trim().isEmpty()) {
            return false;
        }

        String selfTeam = resolveTeamKey(mc.thePlayer.getName());
        String playerTeam = resolveTeamKey(tabPlayerName);
        if (selfTeam.isEmpty() || playerTeam.isEmpty()) {
            return false;
        }
        return !selfTeam.equalsIgnoreCase(playerTeam);
    }

    private String resolveTeamKey(String playerName) {
        if (
            mc == null ||
            mc.theWorld == null ||
            mc.theWorld.getScoreboard() == null ||
            playerName == null ||
            playerName.trim().isEmpty()
        ) {
            return "";
        }

        ScorePlayerTeam team = mc.theWorld.getScoreboard().getPlayersTeam(playerName);
        if (team == null) {
            return "";
        }

        String registeredName = team.getRegisteredName();
        if (registeredName != null && !registeredName.trim().isEmpty()) {
            return registeredName.trim();
        }

        return ChatUtils.stripFormatting(team.getColorPrefix()).trim();
    }

    private String resolveOpponentTeamName(
        String tabPlayerName,
        String fallbackPlayerName
    ) {
        ScorePlayerTeam team = resolveScoreboardTeam(tabPlayerName, fallbackPlayerName);
        if (team == null) {
            return "";
        }

        String fromRegisteredName = normalizeTeamName(team.getRegisteredName());
        if (!fromRegisteredName.isEmpty()) {
            return fromRegisteredName;
        }

        String colorPrefix = team.getColorPrefix();
        String fromPrefixText = normalizeTeamName(
            ChatUtils.stripFormatting(colorPrefix)
        );
        if (!fromPrefixText.isEmpty()) {
            return fromPrefixText;
        }

        return mapColorCodeToTeamName(extractMinecraftColorCode(colorPrefix));
    }

    private ScorePlayerTeam resolveScoreboardTeam(
        String primaryPlayerName,
        String fallbackPlayerName
    ) {
        if (mc == null || mc.theWorld == null || mc.theWorld.getScoreboard() == null) {
            return null;
        }

        if (primaryPlayerName != null && !primaryPlayerName.trim().isEmpty()) {
            ScorePlayerTeam team = mc.theWorld
                .getScoreboard()
                .getPlayersTeam(primaryPlayerName);
            if (team != null) {
                return team;
            }
        }

        if (fallbackPlayerName != null && !fallbackPlayerName.trim().isEmpty()) {
            return mc.theWorld.getScoreboard().getPlayersTeam(fallbackPlayerName);
        }

        return null;
    }

    private String normalizeTeamName(String value) {
        if (value == null) {
            return "";
        }

        String normalized = ChatUtils
            .stripFormatting(value)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z]", "");
        if (normalized.isEmpty()) {
            return "";
        }

        if (
            normalized.equals("r") ||
            normalized.contains("red")
        ) {
            return "Red";
        }
        if (
            normalized.equals("b") ||
            normalized.contains("blue")
        ) {
            return "Blue";
        }
        if (
            normalized.equals("g") ||
            normalized.contains("green")
        ) {
            return "Green";
        }
        if (
            normalized.equals("y") ||
            normalized.contains("yellow")
        ) {
            return "Yellow";
        }
        if (
            normalized.equals("a") ||
            normalized.contains("aqua") ||
            normalized.contains("cyan")
        ) {
            return "Aqua";
        }
        if (
            normalized.equals("w") ||
            normalized.contains("white")
        ) {
            return "White";
        }
        if (
            normalized.equals("p") ||
            normalized.contains("pink") ||
            normalized.contains("lightpurple") ||
            normalized.contains("magenta")
        ) {
            return "Pink";
        }
        if (
            normalized.equals("gr") ||
            normalized.contains("gray") ||
            normalized.contains("grey") ||
            normalized.contains("silver")
        ) {
            return "Gray";
        }

        return "";
    }

    private String mapColorCodeToTeamName(char colorCode) {
        switch (colorCode) {
            case 'c':
            case '4':
                return "Red";
            case '9':
            case '1':
                return "Blue";
            case 'a':
            case '2':
                return "Green";
            case 'e':
            case '6':
                return "Yellow";
            case 'b':
            case '3':
                return "Aqua";
            case 'f':
                return "White";
            case 'd':
            case '5':
                return "Pink";
            case '7':
            case '8':
                return "Gray";
            default:
                return "";
        }
    }

    private char extractMinecraftColorCode(String input) {
        if (input == null || input.length() < 2) {
            return '\0';
        }

        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) == '\u00A7') {
                char maybeColor = Character.toLowerCase(input.charAt(i + 1));
                if (MC_COLOR_CODES.indexOf(maybeColor) >= 0) {
                    return maybeColor;
                }
            }
        }
        return '\0';
    }

    private String formatOutboundBlacklistReason(BlacklistedPlayer blacklistedPlayer) {
        if (blacklistedPlayer == null) {
            return "listed locally";
        }
        String reason = blacklistedPlayer.getReason();
        if (reason == null) {
            return "listed locally";
        }
        String trimmed = reason.trim();
        if (
            trimmed.isEmpty() ||
            BlacklistManager.isExternalFileImportReason(trimmed)
        ) {
            return "listed locally";
        }
        return normalizeOutboundDetail(trimmed);
    }

    private String normalizeOutboundDetail(String detail) {
        if (detail == null) {
            return "unknown reason";
        }

        String normalized = ChatUtils
            .stripFormatting(detail)
            .replace("\r", "")
            .replace("\n", ", ")
            .replace("(null)", "(unknown reason)")
            .replaceAll("\\s+", " ")
            .trim();
        if (normalized.isEmpty()) {
            return "unknown reason";
        }
        return normalized;
    }

    private String formatBlacklistReasonSuffix(String reason) {
        if (reason == null) {
            return "";
        }
        String trimmed = reason.trim();
        if (
            trimmed.isEmpty() ||
            "(none)".equalsIgnoreCase(trimmed) ||
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
