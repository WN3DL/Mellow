package com.roxiun.mellow.feature.stats.tab;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.urchin.UrchinTag;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldSettings;

public class ExtendedStatsTabOverlay extends GuiPlayerTabOverlay {

    private static final Ordering<NetworkPlayerInfo> PLAYER_ORDERING =
        Ordering.from(new PlayerComparator());
    private static final int MAX_TAB_PLAYERS = 80;
    private static final int TOP_Y = 20;
    private static final int BORDER = 4;
    private static final int HEADER_HEIGHT = 12;
    private static final int ENTRY_HEIGHT = 12;

    private final Minecraft mc;
    private final MellowOneConfig config;

    private int scrollIndex;
    private int maxVisiblePlayers = 1;

    public ExtendedStatsTabOverlay(
        Minecraft mcIn,
        GuiIngame guiIngameIn,
        MellowOneConfig config
    ) {
        super(mcIn, guiIngameIn);
        this.mc = mcIn;
        this.config = config;
    }

    public void renderExtendedPlayerList(StatScope scope) {
        if (
            mc == null ||
            mc.thePlayer == null ||
            mc.getNetHandler() == null ||
            mc.getNetHandler().getPlayerInfoMap() == null
        ) {
            return;
        }

        List<NetworkPlayerInfo> players = collectPlayers(mc.getNetHandler());
        if (players.isEmpty()) {
            return;
        }

        List<Integer> columns = ExtendedTabStatsColumns.getConfiguredColumns(
            scope,
            config
        );
        if (columns.isEmpty()) {
            columns = new ArrayList<>(2);
            columns.add(0); // TEAM
            columns.add(2); // NAME
        }

        List<Integer> columnWidths = computeColumnWidths(columns, players, scope);
        int totalWidth = getTotalWidth(columnWidths);

        ScaledResolution scaled = new ScaledResolution(mc);
        int scaledWidth = scaled.getScaledWidth();
        int scaledHeight = scaled.getScaledHeight();

        int startX = Math.max(BORDER, (scaledWidth - totalWidth) / 2);
        int maxRight = scaledWidth - BORDER;
        if (startX + totalWidth > maxRight) {
            startX = Math.max(BORDER, maxRight - totalWidth);
        }

        maxVisiblePlayers =
            Math.max(1, (scaledHeight - (TOP_Y + HEADER_HEIGHT + BORDER)) / (ENTRY_HEIGHT + 1));
        int maxScroll = Math.max(0, players.size() - maxVisiblePlayers);
        scrollIndex = MathHelper.clamp_int(scrollIndex, 0, maxScroll);

        int endIndex = Math.min(players.size(), scrollIndex + maxVisiblePlayers);
        List<NetworkPlayerInfo> visible = players.subList(scrollIndex, endIndex);

        int visibleHeight = visible.size() * (ENTRY_HEIGHT + 1);
        int top = TOP_Y;
        int bottom = top + HEADER_HEIGHT + 1 + visibleHeight;

        drawRect(
            startX - BORDER,
            top - BORDER,
            startX + totalWidth + BORDER,
            bottom + BORDER,
            Integer.MIN_VALUE
        );
        drawRect(startX, top, startX + totalWidth, top + HEADER_HEIGHT, 553648127);

        drawHeaders(columns, columnWidths, scope, startX, top + 2);

        int rowY = top + HEADER_HEIGHT + 1;
        for (NetworkPlayerInfo info : visible) {
            drawRect(startX, rowY, startX + totalWidth, rowY + ENTRY_HEIGHT, 553648127);
            drawValues(columns, columnWidths, scope, info, startX, rowY + ENTRY_HEIGHT / 2 - 4);
            rowY += ENTRY_HEIGHT + 1;
        }

        if (maxScroll > 0) {
            int indicatorX = startX + totalWidth - 8;
            if (scrollIndex > 0) {
                mc.fontRendererObj.drawStringWithShadow("§f▲", indicatorX, top + HEADER_HEIGHT + 2, -1);
            }
            if (endIndex < players.size()) {
                mc.fontRendererObj.drawStringWithShadow("§f▼", indicatorX, bottom - 10, -1);
            }
        }
    }

    public void handleMouseWheel(int wheelDelta, int playerCount) {
        if (wheelDelta == 0) {
            return;
        }

        int effectiveCount = Math.min(playerCount, MAX_TAB_PLAYERS);
        if (effectiveCount <= maxVisiblePlayers) {
            return;
        }

        int maxScroll = Math.max(0, effectiveCount - maxVisiblePlayers);
        if (wheelDelta > 0) {
            scrollIndex--;
        } else {
            scrollIndex++;
        }
        scrollIndex = MathHelper.clamp_int(scrollIndex, 0, maxScroll);
    }

    public void resetScroll() {
        scrollIndex = 0;
        maxVisiblePlayers = 1;
    }

    private List<NetworkPlayerInfo> collectPlayers(NetHandlerPlayClient netHandler) {
        List<NetworkPlayerInfo> sorted = PLAYER_ORDERING.sortedCopy(
            netHandler.getPlayerInfoMap()
        );
        if (sorted.size() <= MAX_TAB_PLAYERS) {
            return sorted;
        }
        return new ArrayList<>(sorted.subList(0, MAX_TAB_PLAYERS));
    }

    private List<Integer> computeColumnWidths(
        List<Integer> columns,
        List<NetworkPlayerInfo> players,
        StatScope scope
    ) {
        List<Integer> widths = new ArrayList<>(columns.size());

        for (int column : columns) {
            String headerLabel = "§l" + ExtendedTabStatsColumns.getHeaderLabel(scope, column) + "§r";
            int width = Math.max(
                ExtendedTabStatsColumns.getMinimumColumnWidth(scope, column),
                mc.fontRendererObj.getStringWidth(headerLabel) + 6
            );

            for (NetworkPlayerInfo info : players) {
                String value = getColumnValue(info, column, scope);
                width = Math.max(width, mc.fontRendererObj.getStringWidth(value) + 6);
            }

            widths.add(width);
        }

        return widths;
    }

    private int getTotalWidth(List<Integer> columnWidths) {
        int total = 0;
        for (int i = 0; i < columnWidths.size(); i++) {
            if (i > 0) {
                total += ExtendedTabStatsColumns.COLUMN_GAP;
            }
            total += columnWidths.get(i);
        }
        return total;
    }

    private void drawHeaders(
        List<Integer> columns,
        List<Integer> columnWidths,
        StatScope scope,
        int startX,
        int y
    ) {
        int x = startX + 1;
        for (int i = 0; i < columns.size(); i++) {
            String header = "§l" + ExtendedTabStatsColumns.getHeaderLabel(scope, columns.get(i)) + "§r";
            mc.fontRendererObj.drawStringWithShadow(header, x, y, -1);
            x += columnWidths.get(i);
            if (i < columns.size() - 1) {
                x += ExtendedTabStatsColumns.COLUMN_GAP;
            }
        }
    }

    private void drawValues(
        List<Integer> columns,
        List<Integer> columnWidths,
        StatScope scope,
        NetworkPlayerInfo info,
        int startX,
        int baselineY
    ) {
        int x = startX + 1;
        for (int i = 0; i < columns.size(); i++) {
            String value = getColumnValue(info, columns.get(i), scope);
            if (value != null && !value.isEmpty()) {
                mc.fontRendererObj.drawStringWithShadow(value, x, baselineY, -1);
            }
            x += columnWidths.get(i);
            if (i < columns.size() - 1) {
                x += ExtendedTabStatsColumns.COLUMN_GAP;
            }
        }
    }

    private String getColumnValue(
        NetworkPlayerInfo info,
        int column,
        StatScope scope
    ) {
        if (info == null || info.getGameProfile() == null) {
            return "";
        }

        String playerName = info.getGameProfile().getName();
        if (playerName == null || playerName.isEmpty()) {
            return "";
        }

        TabStats stats = Mellow.tabStats.get(playerName);
        boolean isNicked =
            Mellow.nickUtils != null && Mellow.nickUtils.isNicked(playerName);

        String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
        String team = tabData != null && tabData.length > 0 ? tabData[0] : "";
        String name = tabData != null && tabData.length > 1 ? tabData[1] : playerName;
        String suffix = tabData != null && tabData.length > 2 ? tabData[2] : "";
        String teamColor = team.length() >= 2 ? team.substring(0, 2) : "§f";

        String value =
            scope == StatScope.SKYWARS
                ? getSkywarsColumnValue(
                    column,
                    team,
                    name,
                    suffix,
                    teamColor,
                    stats,
                    isNicked
                )
                : getBedwarsColumnValue(
                    column,
                    team,
                    name,
                    suffix,
                    teamColor,
                    stats,
                    isNicked
                );

        if (column == 2) {
            value = appendTagSuffixes(value, stats);
            value = appendBlacklistTag(value, info.getGameProfile().getId());
        }

        return value == null ? "" : value;
    }

    private String getBedwarsColumnValue(
        int column,
        String team,
        String name,
        String suffix,
        String teamColor,
        TabStats stats,
        boolean isNicked
    ) {
        switch (column) {
            case 0:
                return team;
            case 1:
                if (isNicked) {
                    return getNickLabel();
                }
                if (stats != null && stats.getStars() != null && !stats.getStars().isEmpty()) {
                    return formatStarsForTab(stats.getStars(), config.showStarsWithBrackets);
                }
                return "";
            case 2:
                if (shouldShowRankInTabName()) {
                    if (
                        stats != null &&
                        stats.getFormattedNameWithRank() != null &&
                        !stats.getFormattedNameWithRank().isEmpty()
                    ) {
                        return stats.getFormattedNameWithRank() + "§r";
                    }
                }
                return "§r" + teamColor + name + suffix;
            case 3:
                return stats != null ? safe(stats.getFkdr()) : "";
            case 4:
                return stats != null ? safe(stats.getWinstreak()) : "";
            case 5:
                return stats != null ? safe(stats.getWlr()) : "";
            case 6:
                return stats != null ? safe(stats.getBblr()) : "";
            case 7:
                return stats != null ? safe(stats.getWins()) : "";
            case 8:
                return stats != null ? safe(stats.getBeds()) : "";
            case 9:
                return stats != null ? safe(stats.getFinals()) : "";
            default:
                return "";
        }
    }

    private String getSkywarsColumnValue(
        int column,
        String team,
        String name,
        String suffix,
        String teamColor,
        TabStats stats,
        boolean isNicked
    ) {
        switch (column) {
            case 0:
                return team;
            case 1:
                if (isNicked) {
                    return getNickLabel();
                }
                if (stats != null && stats.getStars() != null && !stats.getStars().isEmpty()) {
                    return stats.getStars() + "§r";
                }
                return "";
            case 2:
                if (shouldShowRankInTabName()) {
                    if (
                        stats != null &&
                        stats.getFormattedNameWithRank() != null &&
                        !stats.getFormattedNameWithRank().isEmpty()
                    ) {
                        return stats.getFormattedNameWithRank() + "§r";
                    }
                }
                return "§r" + teamColor + name + suffix;
            case 3:
                return stats != null ? safe(stats.getFkdr()) : "";
            case 4:
                return stats != null ? safe(stats.getWlr()) : "";
            case 5:
                return stats != null ? safe(stats.getWins()) : "";
            case 6:
                return stats != null ? safe(stats.getKills()) : "";
            default:
                return "";
        }
    }

    private String appendBlacklistTag(String value, UUID playerUUID) {
        String safe = value == null ? "" : value;
        if (
            playerUUID != null &&
            Mellow.blacklistManager != null &&
            Mellow.blacklistManager.isBlacklisted(playerUUID)
        ) {
            return safe + " §8[§4LIST§8]";
        }
        return safe;
    }

    private String appendTagSuffixes(String value, TabStats stats) {
        if (stats == null) {
            return value;
        }

        String safe = value == null ? "" : value;

        if (Mellow.config.showUrchinTagsInTab && stats.isUrchinTagged()) {
            for (UrchinTag tag : stats.getUrchinTags()) {
                safe += " " + FormattingUtils.formatUrchinTagIcon(tag);
            }
        }

        if (Mellow.config.showSeraphTagsInTab && stats.isSeraphTagged()) {
            for (SeraphTag tag : stats.getSeraphTags()) {
                safe += " " + FormattingUtils.formatSeraphTagIcon(tag);
            }
        }

        return safe;
    }

    private String getNickLabel() {
        if (Mellow.config.showNickWithBrackets) {
            return "§5[§lNICK§r§5]§r";
        }
        return "§5§lNICK§r";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private boolean shouldShowRankInTabName() {
        if (HypixelFeatures.getInstance().getGameSnapshot() == null) {
            return false;
        }
        if (HypixelFeatures.getInstance().getGameSnapshot().isLobby()) {
            return true;
        }
        return Mellow.config.showRanksInGameTabStats;
    }

    private String formatStarsForTab(String stars, boolean withBrackets) {
        if (stars == null || stars.isEmpty()) {
            return "";
        }

        String result;
        if (withBrackets) {
            result = hasOuterBrackets(stars) ? stars : "§7[" + stars + "§7]";
        } else {
            result = stripOuterBrackets(stars);
        }
        return result + "§r";
    }

    private String stripOuterBrackets(String value) {
        if (!hasOuterBrackets(value)) {
            return value;
        }

        int open = value.indexOf('[');
        int close = value.lastIndexOf(']');
        if (open >= 0 && close > open) {
            return (
                value.substring(0, open) +
                value.substring(open + 1, close) +
                value.substring(close + 1)
            );
        }
        return value;
    }

    private boolean hasOuterBrackets(String value) {
        String plain = value.replaceAll("§.", "");
        return plain.startsWith("[") && plain.endsWith("]");
    }

    private static class PlayerComparator implements java.util.Comparator<NetworkPlayerInfo> {

        @Override
        public int compare(NetworkPlayerInfo first, NetworkPlayerInfo second) {
            ScorePlayerTeam firstTeam = first.getPlayerTeam();
            ScorePlayerTeam secondTeam = second.getPlayerTeam();

            return ComparisonChain
                .start()
                .compareTrueFirst(
                    first.getGameType() != WorldSettings.GameType.SPECTATOR,
                    second.getGameType() != WorldSettings.GameType.SPECTATOR
                )
                .compare(
                    firstTeam != null ? firstTeam.getRegisteredName() : "",
                    secondTeam != null ? secondTeam.getRegisteredName() : ""
                )
                .compare(first.getGameProfile().getName(), second.getGameProfile().getName())
                .result();
        }
    }
}
