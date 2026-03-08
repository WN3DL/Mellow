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
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HypixelPublicApi implements StatsProvider {

    private static final String PLAYER_ENDPOINT = "https://api.hypixel.net/v2/player?uuid=";

    private final MojangApi mojangApi;
    private final MellowOneConfig config;

    public HypixelPublicApi(MojangApi mojangApi, MellowOneConfig config) {
        this.mojangApi = mojangApi;
        this.config = config;
    }

    @Override
    public ProviderId getProviderId() {
        return ProviderId.HYPIXEL_PUBLIC;
    }

    @Override
    public String getDisplayName() {
        return "Hypixel Public API";
    }

    @Override
    public boolean requiresApiKey() {
        return true;
    }

    @Override
    public boolean isConfigured() {
        return config != null && config.hypixelApiKey != null && !config.hypixelApiKey.trim().isEmpty();
    }

    @Override
    public String fetchPlayerData(String uuid) {
        ProviderResult<String> result = fetchPlayerDataResult(uuid);
        return result.isSuccess() ? result.getValue() : "";
    }

    @Override
    public ProviderResult<String> fetchPlayerDataResult(String uuid) {
        if (uuid == null || uuid.isEmpty() || !isConfigured()) {
            return ProviderResult.failure(
                isConfigured()
                    ? FetchFailureReason.UUID_UNAVAILABLE
                    : FetchFailureReason.MISSING_API_KEY,
                isConfigured() ? "Missing UUID" : "Missing API key"
            );
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("API-Key", config.hypixelApiKey.trim());

        return HypixelApiUtils.fetchPlayerDataResult(
            PLAYER_ENDPOINT + uuid,
            "Mellow/6.0.0",
            headers
        );
    }

    @Override
    public BedwarsPlayer fetchPlayerStats(String playerName)
        throws IOException {
        if (!isConfigured()) {
            return null;
        }

        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if ("ERROR".equals(uuid)) {
                return null;
            }
        }

        String json = fetchPlayerData(uuid);
        if (json == null || json.isEmpty()) {
            return null;
        }

        return HypixelApiUtils.parsePlayerData(json, ProviderId.HYPIXEL_PUBLIC);
    }

    @Override
    public SkywarsPlayer fetchSkywarsStats(String playerName)
        throws IOException {
        if (!isConfigured()) {
            return null;
        }

        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if ("ERROR".equals(uuid)) {
                return null;
            }
        }

        String json = fetchPlayerData(uuid);
        if (json == null || json.isEmpty()) {
            return null;
        }

        return HypixelApiUtils.parseSkywarsPlayerData(
            json,
            ProviderId.HYPIXEL_PUBLIC
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
        if (!isConfigured()) {
            return null;
        }

        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if ("ERROR".equals(uuid)) {
                return null;
            }
        }

        String json = fetchPlayerData(uuid);
        if (json == null || json.isEmpty()) {
            return null;
        }

        return HypixelApiUtils.parseDuelsPlayerData(
            json,
            ProviderId.HYPIXEL_PUBLIC,
            mode
        );
    }

    @Override
    public BuildBattlePlayer fetchBuildBattleStats(String playerName)
        throws IOException {
        if (!isConfigured()) {
            return null;
        }

        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if ("ERROR".equals(uuid)) {
                return null;
            }
        }

        String json = fetchPlayerData(uuid);
        if (json == null || json.isEmpty()) {
            return null;
        }

        return HypixelApiUtils.parseBuildBattlePlayerData(
            json,
            ProviderId.HYPIXEL_PUBLIC
        );
    }

    @Override
    public TntRunPlayer fetchTntRunStats(String playerName) throws IOException {
        if (!isConfigured()) {
            return null;
        }

        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null) {
            uuid = mojangApi.fetchUUID(playerName);
            if ("ERROR".equals(uuid)) {
                return null;
            }
        }

        String json = fetchPlayerData(uuid);
        if (json == null || json.isEmpty()) {
            return null;
        }

        return HypixelApiUtils.parseTntRunPlayerData(
            json,
            ProviderId.HYPIXEL_PUBLIC
        );
    }
}
