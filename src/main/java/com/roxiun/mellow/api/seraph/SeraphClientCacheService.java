package com.roxiun.mellow.api.seraph;

import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.util.cache.TimedValueCache;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SeraphClientCacheService {

    private static final long CLIENT_LOOKUP_TTL_MS = 300_000L;

    private final SeraphApi seraphApi;
    private final MellowOneConfig config;
    private final TimedValueCache<String, CachedClientLookup> clientCache =
        new TimedValueCache<>(CLIENT_LOOKUP_TTL_MS);
    private final Set<String> fetchInProgress = ConcurrentHashMap.newKeySet();

    public SeraphClientCacheService(
        SeraphApi seraphApi,
        MellowOneConfig config
    ) {
        this.seraphApi = seraphApi;
        this.config = config;
    }

    public SeraphClientType getCachedClient(String playerName) {
        String normalizedName = normalizePlayerName(playerName);
        if (normalizedName.isEmpty()) {
            return null;
        }
        CachedClientLookup cached = clientCache.get(normalizedName);
        return cached == null ? null : cached.clientType;
    }

    public boolean hasCachedClient(String playerName) {
        return clientCache.containsFresh(normalizePlayerName(playerName));
    }

    public void clearCache() {
        clientCache.clear();
        fetchInProgress.clear();
    }

    public void clearPlayer(String playerName) {
        String normalizedName = normalizePlayerName(playerName);
        if (normalizedName.isEmpty()) {
            return;
        }
        clientCache.remove(normalizedName);
        fetchInProgress.remove(normalizedName);
    }

    public void refreshClientAsync(String playerName, String uuid) {
        if (
            playerName == null ||
            playerName.trim().isEmpty() ||
            hasCachedClient(playerName)
        ) {
            return;
        }

        String normalizedName = normalizePlayerName(playerName);
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
        String normalizedName = normalizePlayerName(playerName);
        if (
            normalizedName.isEmpty() ||
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

        SeraphApi.ClientTypeLookupResult result = seraphApi.fetchClientTypeResult(
            uuid,
            normalizeApiKey(config.seraphKey)
        );
        if (!result.isResolved()) {
            clearPlayer(playerName);
            return;
        }

        clientCache.put(
            normalizedName,
            new CachedClientLookup(result.getClientType())
        );
    }

    private String normalizeApiKey(String apiKey) {
        return apiKey == null ? "" : apiKey.trim();
    }

    private String normalizePlayerName(String playerName) {
        return playerName == null
            ? ""
            : playerName.trim().toLowerCase(Locale.ROOT);
    }

    private static final class CachedClientLookup {

        private final SeraphClientType clientType;

        private CachedClientLookup(SeraphClientType clientType) {
            this.clientType = clientType;
        }
    }
}
