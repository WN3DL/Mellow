package com.roxiun.mellow.mixin;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.urchin.UrchinTag;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.util.UUID;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.hypixel.data.type.GameType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GuiPlayerTabOverlay.class)
public class GuiPlayerTabOverlayMixin {

    private static final String MIDDLE_DOT = "\u30fb";
    private static final int BEDWARS_NONE_INDEX = 10;
    private static final int SKYWARS_NONE_INDEX = 7;

    @Inject(method = "getPlayerName", at = @At("HEAD"), cancellable = true)
    public void getPlayerName(
        NetworkPlayerInfo networkPlayerInfoIn,
        CallbackInfoReturnable<String> cir
    ) {
        if (Mellow.config == null || !Mellow.config.tabStats) {
            return;
        }

        String playerName = networkPlayerInfoIn.getGameProfile().getName();
        if (playerName == null) {
            return;
        }

        TabStats stats = Mellow.tabStats.get(playerName);
        boolean isNicked = Mellow.nickUtils.isNicked(playerName);
        String originalDisplayName = getOriginalDisplayName(
            networkPlayerInfoIn
        );
        UUID playerUUID = networkPlayerInfoIn.getGameProfile().getId();

        String newDisplayName;

        if (stats != null) {
            newDisplayName = handlePlayerWithStats(
                playerName,
                stats,
                playerUUID
            );
        } else if (isNicked && !originalDisplayName.contains("§8[§5NICK§8]")) {
            // For nicks without stats, still handle them within the dynamic system
            String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
            if (tabData != null && tabData.length >= 2) {
                String team = tabData[0];
                String name = tabData[1];
                String teamColor = team.length() >= 2
                    ? team.substring(0, 2)
                    : "";

                // Create a minimal TabStats object for the nick case
                TabStats emptyStats = new TabStats(
                    null, // urchinTags
                    null, // seraphTags
                    null, // formattedNameWithRank
                    null, // stars
                    null, // fkdr
                    null, // winstreak
                    null, // wlr
                    null, // bblr
                    null, // wins
                    null, // beds
                    null // finals
                );

                newDisplayName = formatDisplayNameWithStats(
                    team,
                    name,
                    teamColor,
                    emptyStats
                );
            } else {
                // Fallback: create a basic tab structure from network info
                String team = ScorePlayerTeam.formatPlayerName(
                    networkPlayerInfoIn.getPlayerTeam(),
                    playerName
                );
                String name = playerName;
                String teamColor = team.length() >= 2
                    ? team.substring(0, 2)
                    : "";

                // Create a minimal TabStats object for the nick case
                TabStats emptyStats = new TabStats(
                    null, // urchinTags
                    null, // seraphTags
                    null, // formattedNameWithRank
                    null, // stars
                    null, // fkdr
                    null, // winstreak
                    null, // wlr
                    null, // bblr
                    null, // wins
                    null, // beds
                    null // finals
                );

                newDisplayName = formatDisplayNameWithStats(
                    team,
                    name,
                    teamColor,
                    emptyStats
                );
            }
        } else {
            newDisplayName = originalDisplayName;
        }

        newDisplayName = appendBlacklistTag(newDisplayName, playerUUID);

        if (!originalDisplayName.equals(newDisplayName)) {
            cir.setReturnValue(newDisplayName);
        }
    }

    private String handlePlayerWithStats(
        String playerName,
        TabStats stats,
        UUID playerUUID
    ) {
        String[] tabData = PlayerUtils.getTabDisplayName2(playerName);
        if (tabData == null || tabData.length < 2) {
            return "";
        }
        String team = tabData[0];
        String name = tabData[1];

        String teamColor = team.length() >= 2 ? team.substring(0, 2) : "";
        return formatDisplayNameWithStats(team, name, teamColor, stats);
    }

    private String formatDisplayNameWithStats(
        String team,
        String name,
        String teamColor,
        TabStats stats
    ) {
        String newDisplayName = buildOrderedStatsString(
            team,
            name,
            teamColor,
            stats
        );

        if (Mellow.config.showUrchinTagsInTab && stats.isUrchinTagged()) {
            for (UrchinTag tag : stats.getUrchinTags()) {
                newDisplayName +=
                    " " + FormattingUtils.formatUrchinTagIcon(tag);
            }
        }

        if (Mellow.config.showSeraphTagsInTab && stats.isSeraphTagged()) {
            for (SeraphTag tag : stats.getSeraphTags()) {
                newDisplayName +=
                    " " + FormattingUtils.formatSeraphTagIcon(tag);
            }
        }

        return newDisplayName;
    }

    private String buildOrderedStatsString(
        String team,
        String name,
        String teamColor,
        TabStats stats
    ) {
        StatScope scope = resolveTabStatScope();
        return buildDynamicOrderedString(team, name, teamColor, stats, scope);
    }

    private String buildDynamicOrderedString(
        String team,
        String name,
        String teamColor,
        TabStats stats,
        StatScope scope
    ) {
        // Collect all valid stat parts with their type information
        java.util.List<
            java.util.Map.Entry<String, Integer>
        > validPartsWithType = new java.util.ArrayList<>();

        // Process each stat in the configured order with type tracking
        for (int statIndex : getConfiguredStatsForScope(scope)) {
            addValidPartWithConfigStat(
                validPartsWithType,
                statIndex,
                team,
                name,
                teamColor,
                stats,
                scope
            );
        }

        // Build the string with configurable dot separators between positions
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < validPartsWithType.size(); i++) {
            if (i > 0) {
                // Determine which separator to use based on the position and previous element type
                boolean previousIsTeam =
                    validPartsWithType.get(i - 1).getValue() == 0; // Team type is 0

                if (i == 1) {
                    // Between 1st and 2nd (index 0 and 1)
                    if (Mellow.config.showDot12) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 2) {
                    // Between 2nd and 3rd (index 1 and 2)
                    if (Mellow.config.showDot23) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 3) {
                    // Between 3rd and 4th (index 2 and 3)
                    if (Mellow.config.showDot34) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 4) {
                    // Between 4th and 5th (index 3 and 4)
                    if (Mellow.config.showDot45) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 5) {
                    // Between 5th and 6th (index 4 and 5)
                    if (Mellow.config.showDot56) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 6) {
                    // Between 6th and 7th (index 5 and 6)
                    if (Mellow.config.showDot67) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 7) {
                    // Between 7th and 8th (index 6 and 7)
                    if (Mellow.config.showDot78) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 8) {
                    // Between 8th and 9th (index 7 and 8)
                    if (Mellow.config.showDot89) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                } else if (i == 9) {
                    // Between 9th and 10th (index 8 and 9)
                    if (Mellow.config.showDot910) {
                        result.append(MIDDLE_DOT).append("§r");
                    } else if (!previousIsTeam) {
                        result.append(" ");
                    }
                    // If previous is team, don't add any separator since team already has internal spacing
                }
            }
            result.append(validPartsWithType.get(i).getKey());
        }

        return result.toString();
    }

    private void addValidPartWithConfigStat(
        java.util.List<java.util.Map.Entry<String, Integer>> validPartsWithType,
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        StatScope scope
    ) {
        String[] statParts = processDynamicStat(
            statIndex,
            team,
            name,
            teamColor,
            stats,
            scope
        );
        if (statParts != null && !statParts[0].trim().isEmpty()) {
            // Create an entry with the stat value and its type (statIndex)
            validPartsWithType.add(
                new java.util.AbstractMap.SimpleEntry<>(statParts[0], statIndex)
            );
        }
    }

    private String[] processDynamicStat(
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats,
        StatScope scope
    ) {
        if (scope == StatScope.SKYWARS) {
            return processSkywarsDynamicStat(
                statIndex,
                team,
                name,
                teamColor,
                stats
            );
        }
        return processBedwarsDynamicStat(statIndex, team, name, teamColor, stats);
    }

    private String[] processBedwarsDynamicStat(
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats
    ) {
        String stars = stats.getStars();
        String fkdr = stats.getFkdr();

        switch (statIndex) {
            case 0: // Team
                return new String[] { team, "false" };
            case 1: // Stars (shows Nick instead if player is nicks)
                boolean isNicked = Mellow.nickUtils.isNicked(name);
                if (isNicked) {
                    if (Mellow.config.showNickWithBrackets) {
                        return new String[] { "§5[§lNICK§r§5]§r", "false" };
                    } else {
                        return new String[] { "§5§lNICK§r", "false" };
                    }
                } else if (stars != null && !stars.isEmpty()) {
                    return new String[] {
                        formatStarsForTab(
                            stars,
                            Mellow.config.showStarsWithBrackets
                        ),
                        "false",
                    };
                }
                break;
            case 2: // Name
                if (shouldShowRankInTabName()) {
                    String formattedNameWithRank = stats.getFormattedNameWithRank();
                    if (
                        formattedNameWithRank != null &&
                        !formattedNameWithRank.isEmpty()
                    ) {
                        return new String[] { formattedNameWithRank + "§r", "false" };
                    }
                }
                return new String[] { "§r" + teamColor + name, "false" };
            case 3: // FKDR
                if (fkdr != null && !fkdr.isEmpty()) {
                    return new String[] { fkdr, "false" };
                }
                break;
            case 4: // Winstreak
                if (
                    stats.getWinstreak() != null &&
                    !stats.getWinstreak().isEmpty()
                ) {
                    return new String[] { stats.getWinstreak(), "false" };
                }
                break;
            case 5: // WLR
                if (stats.getWlr() != null && !stats.getWlr().isEmpty()) {
                    return new String[] { stats.getWlr(), "false" }; // The color is already included in the string
                }
                break;
            case 6: // BBLR
                if (stats.getBblr() != null && !stats.getBblr().isEmpty()) {
                    return new String[] { stats.getBblr(), "false" }; // The color is already included in the string (if implemented)
                }
                break;
            case 7: // Wins
                if (stats.getWins() != null && !stats.getWins().isEmpty()) {
                    return new String[] { stats.getWins(), "false" }; // The color is already included in the string
                }
                break;
            case 8: // Beds
                if (stats.getBeds() != null && !stats.getBeds().isEmpty()) {
                    return new String[] { stats.getBeds(), "false" }; // The color is already included in the string
                }
                break;
            case 9: // Finals
                if (stats.getFinals() != null && !stats.getFinals().isEmpty()) {
                    return new String[] { stats.getFinals(), "false" }; // The color is already included in the string
                }
                break;
            case BEDWARS_NONE_INDEX: // None
                return null;
        }
        return null;
    }

    private String[] processSkywarsDynamicStat(
        int statIndex,
        String team,
        String name,
        String teamColor,
        TabStats stats
    ) {
        String level = stats.getStars();
        String kdr = stats.getFkdr();

        switch (statIndex) {
            case 0: // Team
                return new String[] { team, "false" };
            case 1: // Level (shows Nick instead if player is nicked)
                boolean isNicked = Mellow.nickUtils.isNicked(name);
                if (isNicked) {
                    if (Mellow.config.showNickWithBrackets) {
                        return new String[] { "§5[§lNICK§r§5]§r", "false" };
                    } else {
                        return new String[] { "§5§lNICK§r", "false" };
                    }
                } else if (level != null && !level.isEmpty()) {
                    return new String[] { level + "§r", "false" };
                }
                break;
            case 2: // Name
                if (shouldShowRankInTabName()) {
                    String formattedNameWithRank = stats.getFormattedNameWithRank();
                    if (
                        formattedNameWithRank != null &&
                        !formattedNameWithRank.isEmpty()
                    ) {
                        return new String[] { formattedNameWithRank + "§r", "false" };
                    }
                }
                return new String[] { "§r" + teamColor + name, "false" };
            case 3: // KDR
                if (kdr != null && !kdr.isEmpty()) {
                    return new String[] { kdr, "false" };
                }
                break;
            case 4: // WLR
                if (stats.getWlr() != null && !stats.getWlr().isEmpty()) {
                    return new String[] { stats.getWlr(), "false" };
                }
                break;
            case 5: // Wins
                if (stats.getWins() != null && !stats.getWins().isEmpty()) {
                    return new String[] { stats.getWins(), "false" };
                }
                break;
            case 6: // Kills
                if (stats.getKills() != null && !stats.getKills().isEmpty()) {
                    return new String[] { stats.getKills(), "false" };
                }
                break;
            case SKYWARS_NONE_INDEX: // None
                return null;
        }

        return null;
    }

    private int[] getConfiguredStatsForScope(StatScope scope) {
        if (scope == StatScope.SKYWARS) {
            return new int[] {
                Mellow.config.skywarsCustomStat1,
                Mellow.config.skywarsCustomStat2,
                Mellow.config.skywarsCustomStat3,
                Mellow.config.skywarsCustomStat4,
                Mellow.config.skywarsCustomStat5,
                Mellow.config.skywarsCustomStat6,
                Mellow.config.skywarsCustomStat7,
                Mellow.config.skywarsCustomStat8,
                Mellow.config.skywarsCustomStat9,
                Mellow.config.skywarsCustomStat10,
            };
        }

        return new int[] {
            Mellow.config.customStat1,
            Mellow.config.customStat2,
            Mellow.config.customStat3,
            Mellow.config.customStat4,
            Mellow.config.customStat5,
            Mellow.config.customStat6,
            Mellow.config.customStat7,
            Mellow.config.customStat8,
            Mellow.config.customStat9,
            Mellow.config.customStat10,
        };
    }

    private String appendBlacklistTag(String displayName, UUID playerUUID) {
        if (
            playerUUID != null &&
            Mellow.blacklistManager.isBlacklisted(playerUUID)
        ) {
            return displayName + " §8[§4LIST§8]";
        }
        return displayName;
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

    private StatScope resolveTabStatScope() {
        if (
            HypixelFeatures.getInstance().getGameSnapshot().getGameType() ==
            GameType.SKYWARS
        ) {
            return StatScope.SKYWARS;
        }
        return StatScope.BEDWARS;
    }

    private String getOriginalDisplayName(
        NetworkPlayerInfo networkPlayerInfoIn
    ) {
        if (networkPlayerInfoIn.getDisplayName() != null) {
            return networkPlayerInfoIn.getDisplayName().getFormattedText();
        }
        return ScorePlayerTeam.formatPlayerName(
            networkPlayerInfoIn.getPlayerTeam(),
            networkPlayerInfoIn.getGameProfile().getName()
        );
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
}
