package com.roxiun.mellow.util.scoreboard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.EnumChatFormatting;

public final class ScoreboardUtils {

    private ScoreboardUtils() {}

    public static String getSidebarTitle() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) {
            return "";
        }

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return "";
        }

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return "";
        }

        String title = EnumChatFormatting.getTextWithoutFormattingCodes(
            objective.getDisplayName()
        );
        return title == null ? "" : title.trim();
    }

    public static List<String> getSidebarLines() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.theWorld == null) {
            return Collections.emptyList();
        }

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return Collections.emptyList();
        }

        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);
        if (objective == null) {
            return Collections.emptyList();
        }

        Collection<Score> allScores = scoreboard.getSortedScores(objective);
        List<Score> filtered = new ArrayList<>();

        for (Score score : allScores) {
            if (score == null || score.getPlayerName() == null) {
                continue;
            }
            if (score.getPlayerName().startsWith("#")) {
                continue;
            }
            filtered.add(score);
        }

        if (filtered.size() > 15) {
            filtered = filtered.subList(filtered.size() - 15, filtered.size());
        }

        List<String> lines = new ArrayList<>();
        for (Score score : filtered) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(
                team,
                score.getPlayerName()
            );
            line = EnumChatFormatting.getTextWithoutFormattingCodes(line);
            if (line == null) {
                continue;
            }

            String cleaned = line.trim();
            if (!cleaned.isEmpty()) {
                lines.add(cleaned);
            }
        }

        Collections.reverse(lines);
        return lines;
    }
}
