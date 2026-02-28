package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.duels.DuelsMode;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.util.HypixelApiUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.IOException;

public class NadeshikoApi implements StatsProvider {

    private final MojangApi mojangApi;

    public NadeshikoApi(MojangApi mojangApi) {
        this.mojangApi = mojangApi;
    }

    @Override
    public ProviderId getProviderId() {
        return ProviderId.NADESHIKO;
    }

    @Override
    public String getDisplayName() {
        return "Nadeshiko";
    }

    @Override
    public String fetchPlayerData(String uuid) {
        return HypixelApiUtils.fetchPlayerData(
            "https://nadeshiko.io/player/" + uuid + "/network",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
        );
    }

    @Override
    public BedwarsPlayer fetchPlayerStats(String playerName)
        throws IOException {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if ("ERROR".equals(uuid)) {
                return null;
            }
        }

        String stjson = fetchPlayerData(uuid);
        if (stjson == null || stjson.isEmpty()) {
            return null;
        }

        return HypixelApiUtils.parsePlayerData(stjson, ProviderId.NADESHIKO);
    }

    @Override
    public SkywarsPlayer fetchSkywarsStats(String playerName)
        throws IOException {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if ("ERROR".equals(uuid)) {
                return null;
            }
        }

        String stjson = fetchPlayerData(uuid);
        if (stjson == null || stjson.isEmpty()) {
            return null;
        }

        return HypixelApiUtils.parseSkywarsPlayerData(
            stjson,
            ProviderId.NADESHIKO
        );
    }

    @Override
    public DuelsPlayer fetchDuelsStats(String playerName)
        throws IOException {
        return fetchDuelsStats(playerName, DuelsMode.OVERALL);
    }

    @Override
    public DuelsPlayer fetchDuelsStats(String playerName, DuelsMode mode)
        throws IOException {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if ("ERROR".equals(uuid)) {
                return null;
            }
        }

        String stjson = fetchPlayerData(uuid);
        if (stjson == null || stjson.isEmpty()) {
            return null;
        }

        return HypixelApiUtils.parseDuelsPlayerData(
            stjson,
            ProviderId.NADESHIKO,
            mode
        );
    }
}
