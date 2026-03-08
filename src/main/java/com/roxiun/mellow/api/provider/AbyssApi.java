package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.buildbattle.BuildBattlePlayer;
import com.roxiun.mellow.api.duels.DuelsMode;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.tnt.TntRunPlayer;
import com.roxiun.mellow.api.util.HypixelApiUtils;
import com.roxiun.mellow.util.cache.TimedValueCache;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.IOException;

public class AbyssApi implements StatsProvider {

    private static final long RAW_DATA_CACHE_TTL_MS = 120_000L;

    private final MojangApi mojangApi;
    private final TimedValueCache<String, ProviderResult<String>> rawDataCache =
        new TimedValueCache<>(RAW_DATA_CACHE_TTL_MS);

    public AbyssApi(MojangApi mojangApi) {
        this.mojangApi = mojangApi;
    }

    @Override
    public ProviderId getProviderId() {
        return ProviderId.ABYSS;
    }

    @Override
    public String getDisplayName() {
        return "Abyss";
    }

    @Override
    public String fetchPlayerData(String uuid) {
        ProviderResult<String> result = fetchPlayerDataResult(uuid);
        return result.isSuccess() ? result.getValue() : "";
    }

    @Override
    public ProviderResult<String> fetchPlayerDataResult(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) {
            return ProviderResult.failure(
                FetchFailureReason.UUID_UNAVAILABLE,
                "Missing UUID"
            );
        }

        String cacheKey = uuid.trim();
        if (rawDataCache.containsFresh(cacheKey)) {
            ProviderResult<String> cached = rawDataCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        ProviderResult<String> result = HypixelApiUtils.fetchPlayerDataResult(
            "http://api.abyssoverlay.com/player?uuid=" + uuid,
            "node-ao/2.0.3"
        );
        if (result.isSuccess()) {
            rawDataCache.put(cacheKey, result);
        }
        return result;
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

        return HypixelApiUtils.parsePlayerData(stjson, ProviderId.ABYSS);
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

        return HypixelApiUtils.parseSkywarsPlayerData(stjson, ProviderId.ABYSS);
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
            ProviderId.ABYSS,
            mode
        );
    }

    @Override
    public BuildBattlePlayer fetchBuildBattleStats(String playerName)
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

        return HypixelApiUtils.parseBuildBattlePlayerData(stjson, ProviderId.ABYSS);
    }

    @Override
    public TntRunPlayer fetchTntRunStats(String playerName) throws IOException {
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

        return HypixelApiUtils.parseTntRunPlayerData(stjson, ProviderId.ABYSS);
    }
}
