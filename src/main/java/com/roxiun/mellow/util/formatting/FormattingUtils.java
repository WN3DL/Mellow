package com.roxiun.mellow.util.formatting;

import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.urchin.UrchinTag;
import java.util.List;
import java.util.stream.Collectors;

public class FormattingUtils {

    public static String formatWinstreak(String text) {
        String color = "§r";
        int winstreak = Integer.parseInt(text);
        if (winstreak >= 20) {
            color = "§4";
        } else if (winstreak >= 10) {
            color = "§6";
        } else if (winstreak >= 5) {
            color = "§b";
        }
        return color + text;
    }

    public static String formatUrchinTags(List<UrchinTag> tags) {
        return tags
            .stream()
            .map(tag -> {
                String type = tag.getType();
                String formattedType;

                // Use exact string matches to avoid substring replacement issues
                switch (type.toLowerCase()) {
                    case "sniper":
                        formattedType = "§4§lSniper";
                        break;
                    case "blatant_cheater":
                        formattedType = "§4§lBlatant Cheater";
                        break;
                    case "closet_cheater":
                        formattedType = "§e§lCloset Cheater";
                        break;
                    case "confirmed_cheater":
                        formattedType = "§4§lConfirmed Cheater";
                        break;
                    case "possible_sniper":
                        formattedType = "§e§lPossible Sniper";
                        break;
                    case "legit_sniper":
                        formattedType = "§e§lLegit Sniper";
                        break;
                    case "caution":
                        formattedType = "§e§lCaution";
                        break;
                    case "account":
                        formattedType = "§e§lAccount";
                        break;
                    case "info":
                        formattedType = "§f§lInfo";
                        break;
                    default:
                        // For unknown types, use the original type as-is
                        formattedType = type;
                        break;
                }

                return formattedType + " §7(" + tag.getReason() + ")";
            })
            .collect(Collectors.joining(", "));
    }

    public static String formatUrchinTagIcon(UrchinTag tag) {
        String type = tag.getType().toLowerCase();
        switch (type) {
            case "sniper":
                return "§8[§4S§8]";
            case "confirmed_cheater":
                return "§8[§cCC§8]";
            case "blatant_cheater":
                return "§8[§4BC§8]";
            case "closet_cheater":
                return "§8[§cCC§8]";
            case "possible_sniper":
                return "§8[§ePS§8]";
            case "legit_sniper":
                return "§8[§3LS§8]";
            case "caution":
                return "§8[§eC§8]";
            default:
                return "";
        }
    }

    public static String formatSeraphTags(List<SeraphTag> tags) {
        return String.join(
            "\n§c",
            tags
                .stream()
                .map(tag -> {
                    // Don't skip unmapped tags - show them using tag name and tooltip
                    if (
                        tag.getTagName() != null &&
                        !tag.getTagName().isEmpty() &&
                        !"seraph.verified".equals(tag.getTagName()) &&
                        !"seraph.advertisement".equals(tag.getTagName())
                    ) {
                        // Format mapped tags nicely, or show unmapped ones with nice formatting
                        String formattedTag = formatSeraphTag(tag.getTagName());
                        if (formattedTag != null && !formattedTag.isEmpty()) {
                            return (
                                formattedTag + " §7(" + tag.getTooltip() + ")"
                            );
                        } else {
                            // For unmapped tags, create a nicely formatted display name
                            String baseName = tag
                                .getTagName()
                                .replace("seraph.", "");
                            String displayName = capitalizeWords(baseName);
                            return (
                                "§7" +
                                displayName +
                                " §7(" +
                                tag.getTooltip() +
                                ")"
                            );
                        }
                    } else if (
                        tag.getTagName() == null || tag.getTagName().isEmpty()
                    ) {
                        // If tag has no tag_name but has tooltip, show it with generic label
                        if (
                            tag.getTooltip() != null &&
                            !tag.getTooltip().isEmpty()
                        ) {
                            return "§7Other §7(" + tag.getTooltip() + ")";
                        } else {
                            return null;
                        }
                    } else {
                        // This is seraph.verified - skip it
                        return null;
                    }
                })
                .filter(tag -> tag != null && !tag.trim().isEmpty())
                .toArray(String[]::new)
        );
    }

    public static String formatSeraphTag(String tagName) {
        if (tagName == null) return "";

        switch (tagName.toLowerCase()) {
            case "seraph.sniping":
            case "seraph.blatant_cheating":
                return "§4§lSniping/Cheating"; // darkred as specified
            case "seraph.legit_sniping":
                return "§c§lLegit Sniper"; // lightred as specified
            case "seraph.potential_sniper":
                return "§e§lPotential Sniper"; // yellow as specified
            case "seraph.bot":
                return "§8§lBot"; // grey as specified
            case "seraph.alt":
                return "§d§lAlt"; // pink as specified
            case "seraph.safelist.personal":
            case "seraph.safelist.group":
            case "seraph.safelist.global":
                return "§a§lSafelist"; // green as specified
            case "seraph.annoylist":
                return "§e§lAnnoying"; // yellow as specified
            case "seraph.encounters":
                return "§c§lEncounters"; // lightred as specified
            case "seraph.cookie":
                return "§c§lEncounters"; // lightred as specified
            case "seraph.caution":
                return "§6§lCaution"; // 0xffc107 = §6 as specified
            case "seraph.closet_cheating":
                return "§e§lCloset Cheater"; // yellow/lightred-like
            default:
                // Skip unmapped tags
                return "";
        }
    }

    public static String formatSeraphTagIcon(SeraphTag tag) {
        String tagName = tag.getTagName().toLowerCase();
        switch (tagName) {
            case "seraph.sniping":
                return "§8[§4S§8]";
            case "seraph.blatant_cheating":
                return "§8[§4BC§8]";
            case "seraph.legit_sniping":
                return "§8[§cLS§8]";
            case "seraph.potential_sniper":
                return "§8[§ePS§8]";
            case "seraph.bot":
                return "§8[§8BOT§8]";
            case "seraph.alt":
                return "§8[§dALT§8]";
            case "seraph.safelist.personal":
            case "seraph.safelist.group":
            case "seraph.safelist.global":
                return "§8[§2§l✓§8]";
            case "seraph.annoylist":
                return "§8[§eAN§8]";
            case "seraph.encounters":
                return "§8[§eSEEN§8]";
            case "seraph.cookie":
                return "§8[§cCOOKIE§8]";
            case "seraph.caution":
                return "§8[§6C§8]";
            case "seraph.closet_cheating":
                return "§8[§eCC§8]";
            default:
                // Skip unmapped tags
                return "";
        }
    }

    public static String formatStars(String text) {
        try {
            return BedwarsStarFormatter.format(Integer.parseInt(text));
        } catch (NumberFormatException e) {
            return "§7[0✫]";
        }
    }

    public static String formatRank(String rank) {
        return rank
            .replace("[VIP", "§a[VIP")
            .replace("[MVP+", "§b[MVP+")
            .replace("[MVP++", "§6[MVP++");
    }

    public static String formatNickedPlayerName(String playerName) {
        net.minecraft.client.Minecraft mc =
            net.minecraft.client.Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return playerName;
        }

        net.minecraft.scoreboard.ScorePlayerTeam playerTeam = mc.theWorld
            .getScoreboard()
            .getPlayersTeam(playerName);
        String[] tabData =
            com.roxiun.mellow.util.player.PlayerUtils.getTabDisplayName2(
                playerName
            );
        String nickedPlayerDisplay;

        if (playerTeam != null && playerTeam.getColorPrefix().length() >= 2) {
            String teamName = playerTeam.getRegisteredName();
            String teamInitial = teamName.substring(0, 1).toUpperCase();
            String teamColor = playerTeam.getColorPrefix().substring(0, 2);

            String teamInfo = teamColor + "§l" + teamInitial + " §r";
            String coloredPlayerName = teamColor + tabData[1] + tabData[2];
            nickedPlayerDisplay = teamInfo + coloredPlayerName;
        } else {
            nickedPlayerDisplay = tabData[0] + tabData[1] + tabData[2];
        }
        return nickedPlayerDisplay;
    }

    private static String capitalizeWords(String input) {
        String[] words = input.split("_");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) result.append(" ");
            if (words[i].length() > 0) {
                result
                    .append(Character.toUpperCase(words[i].charAt(0)))
                    .append(words[i].substring(1).toLowerCase());
            }
        }
        return result.toString();
    }
}
