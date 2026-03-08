package com.roxiun.mellow.api.mojang;

import com.roxiun.mellow.util.cache.TimedValueCache;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class MojangApi {

    private static final long UUID_CACHE_TTL_MS = 300_000L;

    private final TimedValueCache<String, String> uuidCache =
        new TimedValueCache<>(UUID_CACHE_TTL_MS);

    public String fetchUUID(String username) {
        String cacheKey = normalizeUsername(username);
        if (cacheKey.isEmpty()) {
            return "ERROR";
        }
        if (uuidCache.containsFresh(cacheKey)) {
            String cached = uuidCache.get(cacheKey);
            return cached == null ? "ERROR" : cached;
        }

        HttpURLConnection connection = null;
        try {
            String urlString =
                "https://api.minecraftservices.com/minecraft/profile/lookup/name/" +
                username;
            connection = (HttpURLConnection) new URL(
                urlString
            ).openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream())
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();
                String uuid = extractUUID(response.toString());
                if (uuid != null && !"ERROR".equals(uuid)) {
                    return cacheUuid(cacheKey, uuid);
                }
                return "ERROR";
            }

            if (responseCode == 404) {
                return cacheUuid(cacheKey, "ERROR");
            }

            if (responseCode == 429) {
                // Rate limited, fallback to minetools
                HttpURLConnection minetoolsConnection = null;
                try {
                    urlString = "https://api.minetools.eu/uuid/" + username;
                    minetoolsConnection = (HttpURLConnection) new URL(
                        urlString
                    ).openConnection();
                    minetoolsConnection.setRequestMethod("GET");

                    BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                            minetoolsConnection.getInputStream()
                        )
                    );
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) response.append(
                        line
                    );
                    in.close();

                    if (
                        response.toString().contains("\"id\": null")
                    ) {
                        return cacheUuid(cacheKey, "ERROR");
                    }
                    String[] parts = response.toString().split("\"id\":\"");
                    if (parts.length > 1) {
                        return cacheUuid(cacheKey, parts[1].split("\"")[0]);
                    } else {
                        return "ERROR";
                    }
                } finally {
                    if (minetoolsConnection != null) {
                        minetoolsConnection.disconnect();
                    }
                }
            }
        } catch (Exception ignored) {} finally {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return "ERROR";
    }

    private String extractUUID(String response) {
        String[] parts = response.split("\"");
        if (response.contains("Couldn't")) {
            return "ERROR";
        }

        if (parts.length >= 5) {
            return parts[3];
        }

        return null;
    }

    public String getUUIDFromName(String playerName) {
        for (NetworkPlayerInfo info : Minecraft.getMinecraft()
            .getNetHandler()
            .getPlayerInfoMap()) {
            if (info.getGameProfile().getName().equalsIgnoreCase(playerName)) {
                return String.valueOf(info.getGameProfile().getId());
            }
        }
        return null; // Player not found (probably not in tab list)
    }

    public void clearCache() {
        uuidCache.clear();
    }

    public void clearPlayer(String username) {
        String cacheKey = normalizeUsername(username);
        if (cacheKey.isEmpty()) {
            return;
        }
        uuidCache.remove(cacheKey);
    }

    private String cacheUuid(String cacheKey, String uuid) {
        String resolved = uuid == null || uuid.isEmpty() ? "ERROR" : uuid;
        uuidCache.put(cacheKey, resolved);
        return resolved;
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
