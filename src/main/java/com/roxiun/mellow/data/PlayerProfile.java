package com.roxiun.mellow.data;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.buildbattle.BuildBattlePlayer;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.tnt.TntRunPlayer;
import com.roxiun.mellow.api.urchin.UrchinTag;
import java.util.List;

public class PlayerProfile {

    private final String uuid;
    private final String name;
    private final BedwarsPlayer bedwarsPlayer;
    private final SkywarsPlayer skywarsPlayer;
    private final DuelsPlayer duelsPlayer;
    private final BuildBattlePlayer buildBattlePlayer;
    private final TntRunPlayer tntRunPlayer;
    private final List<UrchinTag> urchinTags;
    private final List<SeraphTag> seraphTags;
    private final long lastUpdated;

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        List<UrchinTag> urchinTags
    ) {
        this(uuid, name, bedwarsPlayer, null, null, urchinTags, null);
    }

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        List<UrchinTag> urchinTags,
        List<SeraphTag> seraphTags
    ) {
        this(uuid, name, bedwarsPlayer, null, null, urchinTags, seraphTags);
    }

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        SkywarsPlayer skywarsPlayer,
        List<UrchinTag> urchinTags
    ) {
        this(uuid, name, bedwarsPlayer, skywarsPlayer, null, urchinTags, null);
    }

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        SkywarsPlayer skywarsPlayer,
        DuelsPlayer duelsPlayer,
        List<UrchinTag> urchinTags,
        List<SeraphTag> seraphTags
    ) {
        this(
            uuid,
            name,
            bedwarsPlayer,
            skywarsPlayer,
            duelsPlayer,
            null,
            null,
            urchinTags,
            seraphTags
        );
    }

    public PlayerProfile(
        String uuid,
        String name,
        BedwarsPlayer bedwarsPlayer,
        SkywarsPlayer skywarsPlayer,
        DuelsPlayer duelsPlayer,
        BuildBattlePlayer buildBattlePlayer,
        TntRunPlayer tntRunPlayer,
        List<UrchinTag> urchinTags,
        List<SeraphTag> seraphTags
    ) {
        this.uuid = uuid;
        this.name = name;
        this.bedwarsPlayer = bedwarsPlayer;
        this.skywarsPlayer = skywarsPlayer;
        this.duelsPlayer = duelsPlayer;
        this.buildBattlePlayer = buildBattlePlayer;
        this.tntRunPlayer = tntRunPlayer;
        this.urchinTags = urchinTags;
        this.seraphTags = seraphTags;
        this.lastUpdated = System.currentTimeMillis();
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public BedwarsPlayer getBedwarsPlayer() {
        return bedwarsPlayer;
    }

    public SkywarsPlayer getSkywarsPlayer() {
        return skywarsPlayer;
    }

    public DuelsPlayer getDuelsPlayer() {
        return duelsPlayer;
    }

    public BuildBattlePlayer getBuildBattlePlayer() {
        return buildBattlePlayer;
    }

    public TntRunPlayer getTntRunPlayer() {
        return tntRunPlayer;
    }

    public List<UrchinTag> getUrchinTags() {
        return urchinTags;
    }

    public List<SeraphTag> getSeraphTags() {
        return seraphTags;
    }

    public long getLastUpdated() {
        return lastUpdated;
    }

    public PlayerProfile withTags(
        List<UrchinTag> updatedUrchinTags,
        List<SeraphTag> updatedSeraphTags
    ) {
        return new PlayerProfile(
            uuid,
            name,
            bedwarsPlayer,
            skywarsPlayer,
            duelsPlayer,
            buildBattlePlayer,
            tntRunPlayer,
            updatedUrchinTags,
            updatedSeraphTags
        );
    }

    public boolean isUrchinTagged() {
        return urchinTags != null && !urchinTags.isEmpty();
    }

    public boolean isSeraphTagged() {
        return seraphTags != null && !seraphTags.isEmpty();
    }

    public TabStats getTabStats() {
        return getTabStats(StatScope.BEDWARS);
    }

    public TabStats getTabStats(StatScope scope) {
        if (scope == StatScope.SKYWARS && skywarsPlayer != null) {
            return new TabStats(
                urchinTags,
                seraphTags,
                skywarsPlayer.getFormattedNameWithRank(),
                skywarsPlayer.getLevelFormattedWithBrackets(),
                skywarsPlayer.getFormattedKdrWithColor(),
                null,
                skywarsPlayer.getFormattedWlrWithColor(),
                null,
                skywarsPlayer.getFormattedWinsWithColor(),
                skywarsPlayer.getFormattedKillsWithColor(),
                null,
                null
            );
        }

        if (scope == StatScope.DUELS && duelsPlayer != null) {
            return new TabStats(
                urchinTags,
                seraphTags,
                duelsPlayer.getFormattedNameWithRank(),
                duelsPlayer.getDivision(),
                duelsPlayer.getFormattedKdrWithColor(),
                duelsPlayer.getFormattedWinstreakWithColor(),
                duelsPlayer.getFormattedWlrWithColor(),
                null,
                duelsPlayer.getFormattedWinsWithColor(),
                duelsPlayer.getFormattedLossesWithColor(),
                duelsPlayer.getFormattedKillsWithColor(),
                duelsPlayer.getFormattedDeathsWithColor(),
                null,
                null
            );
        }

        if (scope == StatScope.BUILD_BATTLE && buildBattlePlayer != null) {
            return new TabStats(
                urchinTags,
                seraphTags,
                buildBattlePlayer.getFormattedNameWithRank(),
                buildBattlePlayer.getFormattedTitle(),
                null,
                null,
                null,
                null,
                buildBattlePlayer.getFormattedWinsWithColor(),
                null,
                null,
                null
            );
        }

        if (scope == StatScope.TNT_RUN && tntRunPlayer != null) {
            return new TabStats(
                urchinTags,
                seraphTags,
                tntRunPlayer.getFormattedNameWithRank(),
                null,
                null,
                null,
                tntRunPlayer.getFormattedRatioWithColor(),
                null,
                tntRunPlayer.getFormattedWinsWithColor(),
                tntRunPlayer.getFormattedDeathsWithColor(),
                null,
                null,
                null,
                null
            );
        }

        if (bedwarsPlayer == null) {
            return null;
        }

        // Format numbers with appropriate formatting including colors
        String formattedWins = getBedwarsPlayer().getFormattedWinsWithColor();
        String formattedBeds = getBedwarsPlayer().getFormattedBedsWithColor();
        String formattedFinals =
            getBedwarsPlayer().getFormattedFinalsWithColor();
        String formattedFkdr =
            bedwarsPlayer.getFkdrColor() + bedwarsPlayer.getFormattedFkdr();
        String formattedWinstreak =
            getBedwarsPlayer().getFormattedWinstreakWithColor();
        String formattedWLR = getBedwarsPlayer().getFormattedWLRWithColor();
        String formattedBBLR = getBedwarsPlayer().getFormattedBBLRWithColor();

        return new TabStats(
            urchinTags,
            seraphTags,
            bedwarsPlayer.getFormattedNameWithRank(),
            bedwarsPlayer.getStars(),
            formattedFkdr,
            formattedWinstreak,
            formattedWLR,
            formattedBBLR,
            formattedWins,
            null,
            formattedBeds,
            formattedFinals
        );
    }
}
