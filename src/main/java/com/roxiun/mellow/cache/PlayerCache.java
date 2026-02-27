package com.roxiun.mellow.cache;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.provider.ProviderManager;
import com.roxiun.mellow.api.provider.StatsProvider;
import com.roxiun.mellow.api.seraph.SeraphApi;
import com.roxiun.mellow.api.seraph.SeraphTag;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
import com.roxiun.mellow.api.urchin.UrchinApi;
import com.roxiun.mellow.api.urchin.UrchinTag;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;

public class PlayerCache {

    private static final long CACHE_TTL_MS = 120_000L;

    private final Map<String, CachedProfile> cache = new ConcurrentHashMap<>();
    private final MojangApi mojangApi;
    private final ProviderManager providerManager;
    private final UrchinApi urchinApi;
    private final SeraphApi seraphApi;
    private final String urchinApiKey;
    private final String seraphApiKey;
    private final MellowOneConfig config;

    private long lastMissingApiKeyWarnAt;

    public PlayerCache(
        MojangApi mojangApi,
        ProviderManager providerManager,
        UrchinApi urchinApi,
        SeraphApi seraphApi,
        String urchinApiKey,
        String seraphApiKey,
        MellowOneConfig config
    ) {
        this.mojangApi = mojangApi;
        this.providerManager = providerManager;
        this.urchinApi = urchinApi;
        this.seraphApi = seraphApi;
        this.urchinApiKey = urchinApiKey;
        this.seraphApiKey = seraphApiKey;
        this.config = config;
    }

    public PlayerProfile getProfile(String playerName) {
        StatsProvider provider = providerManager.getSelectedProvider(config);
        if (provider == null) {
            return null;
        }

        String cacheKey = provider.getProviderId().name() + ":" + playerName.toLowerCase();
        CachedProfile cached = cache.get(cacheKey);

        if (cached != null && !cached.isExpired()) {
            return cached.profile;
        }

        if (provider.requiresApiKey() && !provider.isConfigured()) {
            maybeWarnMissingApiKey(provider.getDisplayName());
            return null;
        }

        return fetchAndCachePlayer(playerName, provider, cacheKey);
    }

    public StatsProvider getSelectedProvider() {
        return providerManager.getSelectedProvider(config);
    }

    public String fetchRawPlayerData(String playerName) {
        StatsProvider provider = providerManager.getSelectedProvider(config);
        if (provider == null) {
            return "";
        }
        if (provider.requiresApiKey() && !provider.isConfigured()) {
            maybeWarnMissingApiKey(provider.getDisplayName());
            return "";
        }

        String uuid = resolveUuid(playerName);
        if (uuid == null || uuid.isEmpty() || "ERROR".equals(uuid)) {
            return "";
        }

        try {
            String raw = provider.fetchPlayerData(uuid);
            return raw == null ? "" : raw;
        } catch (Exception ignored) {
            return "";
        }
    }

    private void maybeWarnMissingApiKey(String providerName) {
        long now = System.currentTimeMillis();
        if (now - lastMissingApiKeyWarnAt < 10_000L) {
            return;
        }

        lastMissingApiKeyWarnAt = now;
        Minecraft.getMinecraft().addScheduledTask(() ->
            ChatUtils.sendMessage(
                "§e" +
                providerName +
                " is selected but no API key is configured. Stats are disabled until a key is set in OneConfig."
            )
        );
    }

    private PlayerProfile fetchAndCachePlayer(
        String playerName,
        StatsProvider provider,
        String cacheKey
    ) {
        try {
            BedwarsPlayer bedwarsPlayer = provider.fetchPlayerStats(playerName);
            SkywarsPlayer skywarsPlayer = provider.fetchSkywarsStats(playerName);
            if (bedwarsPlayer == null && skywarsPlayer == null) {
                return null;
            }

            String uuid = resolveUuid(playerName);

            if (uuid == null || uuid.isEmpty() || "ERROR".equals(uuid)) {
                return null;
            }

            List<UrchinTag> urchinTags = null;
            if (config.urchin) {
                try {
                    urchinTags = urchinApi.fetchUrchinTags(
                        uuid,
                        playerName,
                        urchinApiKey
                    );
                } catch (IOException e) {
                    Minecraft.getMinecraft().addScheduledTask(() ->
                        ChatUtils.sendMessage(
                            "§cFailed to fetch Urchin tags for " + playerName + "."
                        )
                    );
                }
            }

            List<SeraphTag> seraphTags = null;
            if (config.seraph) {
                try {
                    seraphTags = seraphApi.fetchSeraphTags(uuid, seraphApiKey);
                } catch (IOException e) {
                    Minecraft.getMinecraft().addScheduledTask(() ->
                        ChatUtils.sendMessage(
                            "§cFailed to fetch Seraph tags for " + playerName + "."
                        )
                    );
                }
            }

            PlayerProfile newProfile = new PlayerProfile(
                uuid,
                playerName,
                bedwarsPlayer,
                skywarsPlayer,
                urchinTags,
                seraphTags
            );

            cache.put(cacheKey, new CachedProfile(newProfile));
            return newProfile;
        } catch (Exception e) {
            return null;
        }
    }

    public void clearCache() {
        cache.clear();
    }

    public void clearPlayer(String playerName) {
        String lower = playerName.toLowerCase();
        cache.keySet().removeIf(key -> key.endsWith(":" + lower));
    }

    private String resolveUuid(String playerName) {
        String uuid = PlayerUtils.getUUIDFromPlayerName(playerName);
        if (uuid == null || uuid.isEmpty()) {
            uuid = mojangApi.fetchUUID(playerName);
        }
        return uuid;
    }

    private static class CachedProfile {

        private final PlayerProfile profile;
        private final long cachedAt;

        private CachedProfile(PlayerProfile profile) {
            this.profile = profile;
            this.cachedAt = System.currentTimeMillis();
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - cachedAt > CACHE_TTL_MS;
        }
    }
}
