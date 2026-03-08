package com.roxiun.mellow.api.seraph;

import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SeraphClientCacheService {

    private final SeraphApi seraphApi;
    private final MellowOneConfig config;
    private final Map<String, SeraphClientType> clientCache =
        new ConcurrentHashMap<>();
    private final Set<String> fetchInProgress = ConcurrentHashMap.newKeySet();

    public SeraphClientCacheService(
        SeraphApi seraphApi,
        MellowOneConfig config
    ) {
        this.seraphApi = seraphApi;
        this.config = config;
    }

    public SeraphClientType getCachedClient(String playerName) {
        if (playerName == null) {
            return null;
        }
        return clientCache.get(playerName);
    }

    public boolean hasCachedClient(String playerName) {
        if (playerName == null) {
            return false;
        }
        return clientCache.containsKey(playerName);
    }

    public void clearCache() {
        clientCache.clear();
        fetchInProgress.clear();
    }

    public void clearPlayer(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            return;
        }
        clientCache.remove(playerName);
        fetchInProgress.remove(playerName.toLowerCase(Locale.ROOT));
    }

    public void refreshClientAsync(String playerName, String uuid) {
        if (
            playerName == null ||
            playerName.trim().isEmpty() ||
            hasCachedClient(playerName)
        ) {
            return;
        }

        String normalizedName = playerName.toLowerCase(Locale.ROOT);
        if (!fetchInProgress.add(normalizedName)) {
            return;
        }

        AsyncExecutor.getInstance().supplementalIo(() -> {
            try {
                refreshClient(playerName, uuid);
            } finally {
                fetchInProgress.remove(normalizedName);
            }
        });
    }

    public void refreshClient(String playerName, String uuid) {
        if (
            playerName == null ||
            playerName.trim().isEmpty() ||
            config == null ||
            seraphApi == null ||
            !config.seraph
        ) {
            clearPlayer(playerName);
            return;
        }

        if (uuid == null || uuid.trim().isEmpty() || "ERROR".equals(uuid)) {
            clearPlayer(playerName);
            return;
        }

        SeraphClientType clientType = seraphApi.fetchClientType(
            uuid,
            normalizeApiKey(config.seraphKey)
        );
        if (clientType == null) {
            clearPlayer(playerName);
            return;
        }

        clientCache.put(playerName, clientType);
    }

    private String normalizeApiKey(String apiKey) {
        return apiKey == null ? "" : apiKey.trim();
    }
}
