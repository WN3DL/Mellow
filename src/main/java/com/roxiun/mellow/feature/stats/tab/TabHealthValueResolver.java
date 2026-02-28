package com.roxiun.mellow.feature.stats.tab;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.IScoreObjectiveCriteria;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.util.MathHelper;

public final class TabHealthValueResolver {

    private static final int MAX_CACHE_SIZE = 512;
    private static final String UNKNOWN_HP = "§7--";

    private static final Map<UUID, Integer> LAST_KNOWN_HP_BY_UUID =
        new HashMap<>();
    private static final Map<String, Integer> LAST_KNOWN_HP_BY_NAME =
        new HashMap<>();

    private TabHealthValueResolver() {}

    public static String getFormattedHealth(
        Minecraft mc,
        NetworkPlayerInfo playerInfo
    ) {
        if (playerInfo == null || playerInfo.getGameProfile() == null) {
            return UNKNOWN_HP;
        }

        return getFormattedHealth(
            mc,
            playerInfo.getGameProfile().getName(),
            playerInfo.getGameProfile().getId()
        );
    }

    public static String getFormattedHealth(
        Minecraft mc,
        String playerName,
        UUID playerUuid
    ) {
        Integer hp = resolveHealth(mc, playerName, playerUuid);
        if (hp == null) {
            return UNKNOWN_HP;
        }
        return colorize(hp);
    }

    private static Integer resolveHealth(
        Minecraft mc,
        String playerName,
        UUID playerUuid
    ) {
        Integer fromEntity = resolveFromEntity(mc, playerName, playerUuid);
        if (fromEntity != null) {
            remember(playerName, playerUuid, fromEntity);
            return fromEntity;
        }

        Integer fromScoreboard = resolveFromPlayerListScore(mc, playerName);
        if (fromScoreboard != null) {
            remember(playerName, playerUuid, fromScoreboard);
            return fromScoreboard;
        }

        return getLastKnown(playerName, playerUuid);
    }

    private static Integer resolveFromEntity(
        Minecraft mc,
        String playerName,
        UUID playerUuid
    ) {
        if (mc == null || mc.theWorld == null) {
            return null;
        }

        EntityPlayer entity = null;
        if (playerUuid != null) {
            entity = mc.theWorld.getPlayerEntityByUUID(playerUuid);
        }
        if (entity == null && playerName != null && !playerName.isEmpty()) {
            entity = mc.theWorld.getPlayerEntityByName(playerName);
        }
        if (entity == null) {
            return null;
        }

        float totalHealth = entity.getHealth() + entity.getAbsorptionAmount();
        return Math.max(0, MathHelper.ceiling_float_int(totalHealth));
    }

    private static Integer resolveFromPlayerListScore(
        Minecraft mc,
        String playerName
    ) {
        if (
            mc == null ||
            mc.theWorld == null ||
            playerName == null ||
            playerName.isEmpty()
        ) {
            return null;
        }

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        if (scoreboard == null) {
            return null;
        }

        ScoreObjective playerListObjective = scoreboard.getObjectiveInDisplaySlot(
            0
        );
        if (playerListObjective == null) {
            return null;
        }
        if (
            playerListObjective.getRenderType() !=
            IScoreObjectiveCriteria.EnumRenderType.HEARTS
        ) {
            return null;
        }

        Collection<Score> scores = scoreboard.getSortedScores(playerListObjective);
        if (scores == null || scores.isEmpty()) {
            return null;
        }

        for (Score score : scores) {
            if (score == null) {
                continue;
            }
            if (playerName.equalsIgnoreCase(score.getPlayerName())) {
                return Math.max(0, score.getScorePoints());
            }
        }
        return null;
    }

    private static synchronized void remember(
        String playerName,
        UUID playerUuid,
        int health
    ) {
        if (playerUuid != null) {
            LAST_KNOWN_HP_BY_UUID.put(playerUuid, health);
            if (LAST_KNOWN_HP_BY_UUID.size() > MAX_CACHE_SIZE) {
                LAST_KNOWN_HP_BY_UUID.clear();
            }
        }

        if (playerName != null && !playerName.isEmpty()) {
            LAST_KNOWN_HP_BY_NAME.put(
                playerName.toLowerCase(Locale.ROOT),
                health
            );
            if (LAST_KNOWN_HP_BY_NAME.size() > MAX_CACHE_SIZE) {
                LAST_KNOWN_HP_BY_NAME.clear();
            }
        }
    }

    private static synchronized Integer getLastKnown(
        String playerName,
        UUID playerUuid
    ) {
        if (playerUuid != null) {
            Integer byUuid = LAST_KNOWN_HP_BY_UUID.get(playerUuid);
            if (byUuid != null) {
                return byUuid;
            }
        }

        if (playerName != null && !playerName.isEmpty()) {
            return LAST_KNOWN_HP_BY_NAME.get(
                playerName.toLowerCase(Locale.ROOT)
            );
        }

        return null;
    }

    private static String colorize(int hp) {
        if (hp >= 16) {
            return "§a" + hp;
        }
        if (hp >= 11) {
            return "§e" + hp;
        }
        if (hp >= 6) {
            return "§6" + hp;
        }
        return "§c" + hp;
    }
}
