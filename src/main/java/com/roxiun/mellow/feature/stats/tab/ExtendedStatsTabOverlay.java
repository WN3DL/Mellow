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
import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.util.MathHelper;
import net.minecraft.world.WorldSettings;

public class ExtendedStatsTabOverlay extends GuiPlayerTabOverlay {

    private static final Ordering<NetworkPlayerInfo> PLAYER_ORDERING =
        Ordering.from(new PlayerComparator());
    private static final int MAX_TAB_PLAYERS = 80;
    private static final int TOP_Y = 20;
    private static final int BORDER = 4;
    private static final int HEADER_HEIGHT = 11;
    private static final int ENTRY_HEIGHT = 11;
    private static final int ROW_GAP = 1;
    private static final int CELL_PADDING_X = 3;
    private static final int COLUMN_HEALTH = -1;
    private static final int HEALTH_POS_AFTER_NAME = 0;
    private static final int HEALTH_POS_FAR_RIGHT = 1;
    private static final int HEAD_ICON_SIZE = 8;
    private static final int HEAD_TEXT_GAP = 2;

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
        columns = withInjectedExtendedColumns(columns);

        List<Integer> columnWidths = computeColumnWidths(columns, players, scope);
        int totalWidth = getTotalWidth(columnWidths);

        ScaledResolution scaled = new ScaledResolution(mc);
        int scaledWidth = scaled.getScaledWidth();
        int scaledHeight = scaled.getScaledHeight();
        int availableWidth = Math.max(100, scaledWidth - BORDER * 2);
        float fitScale = totalWidth > availableWidth
            ? (float) availableWidth / (float) totalWidth
            : 1.0F;

        int scaledPanelWidth = MathHelper.ceiling_float_int(totalWidth * fitScale);
        int startX = Math.max(BORDER, (scaledWidth - scaledPanelWidth) / 2);
        int maxRight = scaledWidth - BORDER;
        if (startX + scaledPanelWidth > maxRight) {
            startX = Math.max(BORDER, maxRight - scaledPanelWidth);
        }

        int scaledHeader = Math.max(
            1,
            MathHelper.ceiling_float_int((HEADER_HEIGHT + ROW_GAP) * fitScale)
        );
        int scaledRowStep = Math.max(
            1,
            MathHelper.ceiling_float_int((ENTRY_HEIGHT + ROW_GAP) * fitScale)
        );
        maxVisiblePlayers = Math.max(
            1,
            (scaledHeight - (TOP_Y + scaledHeader + BORDER * 2)) / scaledRowStep
        );
        int maxScroll = Math.max(0, players.size() - maxVisiblePlayers);
        scrollIndex = MathHelper.clamp_int(scrollIndex, 0, maxScroll);

        int endIndex = Math.min(players.size(), scrollIndex + maxVisiblePlayers);
        List<NetworkPlayerInfo> visible = players.subList(scrollIndex, endIndex);

        int visibleHeight = visible.size() * (ENTRY_HEIGHT + ROW_GAP);
        int panelContentHeight = HEADER_HEIGHT + ROW_GAP + visibleHeight;

        GlStateManager.pushMatrix();
        GlStateManager.translate(startX, TOP_Y, 0.0F);
        GlStateManager.scale(fitScale, fitScale, 1.0F);

        drawRect(-BORDER, -BORDER, totalWidth + BORDER, panelContentHeight + BORDER, Integer.MIN_VALUE);
        drawRect(0, 0, totalWidth, HEADER_HEIGHT, 553648127);

        drawHeaders(columns, columnWidths, scope, 0, (HEADER_HEIGHT - mc.fontRendererObj.FONT_HEIGHT) / 2);

        int rowY = HEADER_HEIGHT + ROW_GAP;
        for (NetworkPlayerInfo info : visible) {
            drawRect(0, rowY, totalWidth, rowY + ENTRY_HEIGHT, 553648127);
            drawValues(
                columns,
                columnWidths,
                scope,
                info,
                0,
                rowY + (ENTRY_HEIGHT - mc.fontRendererObj.FONT_HEIGHT) / 2
            );
            rowY += ENTRY_HEIGHT + ROW_GAP;
        }

        if (maxScroll > 0) {
            int indicatorX = totalWidth - 8;
            if (scrollIndex > 0) {
                mc.fontRendererObj.drawStringWithShadow("§f▲", indicatorX, HEADER_HEIGHT + 1, -1);
            }
            if (endIndex < players.size()) {
                mc.fontRendererObj.drawStringWithShadow(
                    "§f▼",
                    indicatorX,
                    panelContentHeight - mc.fontRendererObj.FONT_HEIGHT - 1,
                    -1
                );
            }
        }

        GlStateManager.popMatrix();
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

    private List<Integer> withInjectedExtendedColumns(List<Integer> baseColumns) {
        List<Integer> result = new ArrayList<>(baseColumns);
        if (config == null || !config.extendedTabStatsShowHealth) {
            return result;
        }

        if (result.contains(COLUMN_HEALTH)) {
            return result;
        }

        int insertAt;
        if (config.extendedTabStatsHealthPosition == HEALTH_POS_AFTER_NAME) {
            int nameIndex = result.indexOf(2);
            insertAt = nameIndex >= 0 ? nameIndex + 1 : Math.min(1, result.size());
        } else if (config.extendedTabStatsHealthPosition == HEALTH_POS_FAR_RIGHT) {
            insertAt = result.size();
        } else {
            insertAt = result.size();
        }
        result.add(insertAt, COLUMN_HEALTH);
        return result;
    }

    private List<Integer> computeColumnWidths(
        List<Integer> columns,
        List<NetworkPlayerInfo> players,
        StatScope scope
    ) {
        List<Integer> widths = new ArrayList<>(columns.size());

        for (int column : columns) {
            String headerLabel = "§l" + getHeaderLabel(scope, column) + "§r";
            int width = Math.max(
                getMinimumColumnWidth(scope, column),
                mc.fontRendererObj.getStringWidth(headerLabel) + CELL_PADDING_X * 2
            );

            for (NetworkPlayerInfo info : players) {
                String value = getColumnValue(info, column, scope);
                int extra =
                    column == 2 && shouldShowHeadsInExtendedView()
                        ? HEAD_ICON_SIZE + HEAD_TEXT_GAP
                        : 0;
                width = Math.max(
                    width,
                    mc.fontRendererObj.getStringWidth(value) + CELL_PADDING_X * 2 + extra
                );
            }

            width = Math.min(width, getMaximumColumnWidth(scope, column));
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
        int x = startX;
        for (int i = 0; i < columns.size(); i++) {
            String header = "§l" + getHeaderLabel(scope, columns.get(i)) + "§r";
            int column = columns.get(i);
            int width = columnWidths.get(i);
            int headerWidth = mc.fontRendererObj.getStringWidth(header);
            int drawX =
                isRightAlignedColumn(column)
                    ? x + width - CELL_PADDING_X - headerWidth
                    : x + CELL_PADDING_X + (column == 2 && shouldShowHeadsInExtendedView()
                        ? HEAD_ICON_SIZE + HEAD_TEXT_GAP
                        : 0);
            mc.fontRendererObj.drawStringWithShadow(header, drawX, y, -1);
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
        int x = startX;
        for (int i = 0; i < columns.size(); i++) {
            int column = columns.get(i);
            int width = columnWidths.get(i);
            int textStartX = x + CELL_PADDING_X;
            int reservedLeft = CELL_PADDING_X * 2;

            if (column == 2 && shouldShowHeadsInExtendedView()) {
                int headX = x + CELL_PADDING_X;
                int headY = baselineY + (mc.fontRendererObj.FONT_HEIGHT - HEAD_ICON_SIZE) / 2;
                drawPlayerHead(info, headX, headY, HEAD_ICON_SIZE);
                textStartX += HEAD_ICON_SIZE + HEAD_TEXT_GAP;
                reservedLeft += HEAD_ICON_SIZE + HEAD_TEXT_GAP;
            }

            int maxTextWidth = Math.max(1, width - reservedLeft);

            String value = fitToWidth(getColumnValue(info, column, scope), maxTextWidth);
            if (value != null && !value.isEmpty()) {
                int drawX =
                    isRightAlignedColumn(column)
                        ? x + width - CELL_PADDING_X - mc.fontRendererObj.getStringWidth(value)
                        : textStartX;
                mc.fontRendererObj.drawStringWithShadow(value, drawX, baselineY, -1);
            }
            x += width;
            if (i < columns.size() - 1) {
                x += ExtendedTabStatsColumns.COLUMN_GAP;
            }
        }
    }

    private String fitToWidth(String value, int width) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        if (mc.fontRendererObj.getStringWidth(value) <= width) {
            return value;
        }

        String suffix = "§7...";
        int suffixWidth = mc.fontRendererObj.getStringWidth(suffix);
        int trimmedWidth = Math.max(0, width - suffixWidth);
        String trimmed = mc.fontRendererObj.trimStringToWidth(value, trimmedWidth);
        if (trimmed == null || trimmed.isEmpty()) {
            return "";
        }
        return trimmed + suffix;
    }

    private void drawPlayerHead(
        NetworkPlayerInfo playerInfo,
        int x,
        int y,
        int size
    ) {
        if (playerInfo == null || playerInfo.getGameProfile() == null) {
            return;
        }

        if (playerInfo.getLocationSkin() == null) {
            return;
        }

        GameProfile gameProfile = playerInfo.getGameProfile();
        EntityPlayer entityPlayer = mc.theWorld == null
            ? null
            : mc.theWorld.getPlayerEntityByUUID(gameProfile.getId());
        boolean upsideDown =
            entityPlayer != null &&
            entityPlayer.isWearing(EnumPlayerModelParts.CAPE) &&
            ("Dinnerbone".equals(gameProfile.getName()) ||
                "Grumm".equals(gameProfile.getName()));

        int vBase = 8 + (upsideDown ? 8 : 0);
        int vSize = 8 * (upsideDown ? -1 : 1);

        mc.getTextureManager().bindTexture(playerInfo.getLocationSkin());
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        Gui.drawScaledCustomSizeModalRect(
            x,
            y,
            8.0F,
            (float) vBase,
            8,
            vSize,
            size,
            size,
            64.0F,
            64.0F
        );

        if (entityPlayer != null && entityPlayer.isWearing(EnumPlayerModelParts.HAT)) {
            Gui.drawScaledCustomSizeModalRect(
                x,
                y,
                40.0F,
                (float) vBase,
                8,
                vSize,
                size,
                size,
                64.0F,
                64.0F
            );
        }
    }

    private boolean isRightAlignedColumn(int column) {
        return column != 0 && column != 2;
    }

    private String getHeaderLabel(StatScope scope, int column) {
        if (column == COLUMN_HEALTH) {
            return "HP";
        }
        return ExtendedTabStatsColumns.getHeaderLabel(scope, column);
    }

    private int getMinimumColumnWidth(StatScope scope, int column) {
        if (column == COLUMN_HEALTH) {
            return 18;
        }

        switch (column) {
            case 0: // TEAM
                return 18;
            case 1: // STARS / LEVEL
                return 22;
            case 2: // NAME
                return shouldShowHeadsInExtendedView() ? 70 : 58;
            default: // numeric stat columns
                return 18;
        }
    }

    private int getMaximumColumnWidth(StatScope scope, int column) {
        if (column == COLUMN_HEALTH) {
            return 34;
        }

        if (scope == StatScope.SKYWARS) {
            switch (column) {
                case 0:
                    return 40;
                case 1:
                    return 70;
                case 2:
                    return shouldShowHeadsInExtendedView() ? 230 : 220;
                default:
                    return 72;
            }
        }

        switch (column) {
            case 0:
                return 40;
            case 1:
                return 70;
            case 2:
                return shouldShowHeadsInExtendedView() ? 230 : 220;
            default:
                return 72;
        }
    }

    private boolean shouldShowHeadsInExtendedView() {
        return config != null && config.extendedTabStatsShowHeads;
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

        if (column == COLUMN_HEALTH) {
            return getHealthValue(info);
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

    private String getHealthValue(NetworkPlayerInfo info) {
        if (
            info == null ||
            info.getGameProfile() == null ||
            info.getGameProfile().getId() == null ||
            mc == null ||
            mc.theWorld == null
        ) {
            return "";
        }

        EntityPlayer entity = mc.theWorld.getPlayerEntityByUUID(
            info.getGameProfile().getId()
        );
        if (entity == null) {
            return "";
        }

        float totalHealth = entity.getHealth() + entity.getAbsorptionAmount();
        int hp = Math.max(0, MathHelper.ceiling_float_int(totalHealth));

        String color;
        if (hp >= 16) {
            color = "§a";
        } else if (hp >= 11) {
            color = "§e";
        } else if (hp >= 6) {
            color = "§6";
        } else {
            color = "§c";
        }
        return color + hp;
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
