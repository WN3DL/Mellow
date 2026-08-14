package com.roxiun.mellow.api.mojang;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.cache.TimedValueCache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class MojangApi {

    private static final long UUID_CACHE_TTL_MS = 300_000L;
    private static final String MINECRAFT_PROFILE_URL =
        "https://api.minecraftservices.com/minecraft/profile/lookup/name/";
    private static final String SERAPH_MOJANG_URL =
        "https://mowojang.seraph.si/";
    private static final String MINETOOLS_UUID_URL =
        "https://api.minetools.eu/uuid/";

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

        try {
            HttpResult result = executeGetRequest(
                new URL(MINECRAFT_PROFILE_URL + username)
            );
            if (result.statusCode == HttpURLConnection.HTTP_OK) {
                String uuid = extractUuid(result.body);
                return uuid.isEmpty() ? "ERROR" : cacheUuid(cacheKey, uuid);
            }
            if (result.statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                return cacheUuid(cacheKey, "ERROR");
            }
        } catch (Exception ignored) {}

        MojangProfile seraphProfile = fetchSeraphMojang(username);
        if (seraphProfile != null) {
            return cacheUuid(cacheKey, toUndashedUuid(seraphProfile.uuid));
        }

        try {
            HttpResult result = executeGetRequest(
                new URL(MINETOOLS_UUID_URL + username)
            );
            if (result.statusCode == HttpURLConnection.HTTP_OK) {
                String uuid = extractUuid(result.body);
                if (!uuid.isEmpty()) {
                    return cacheUuid(cacheKey, uuid);
                }
            }
        } catch (Exception ignored) {}

        return "ERROR";
    }

    public MojangProfile fetchSeraphMojang(String nameOrId) {
        if (nameOrId == null || nameOrId.trim().isEmpty()) {
            return null;
        }

        try {
            HttpResult result = executeGetRequest(
                new URL(SERAPH_MOJANG_URL + nameOrId.trim())
            );
            if (result.statusCode != HttpURLConnection.HTTP_OK) {
                return null;
            }

            JsonObject json = new JsonParser()
                .parse(result.body)
                .getAsJsonObject();
            String name = getJsonString(json, "name");
            String uuid = extractUuid(json);
            if (name.isEmpty() || uuid.isEmpty()) {
                return null;
            }
            return new MojangProfile(name, UUIDUtils.fromString(uuid));
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractUuid(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }
        try {
            return extractUuid(
                new JsonParser().parse(response).getAsJsonObject()
            );
        } catch (Exception ignored) {
            return "";
        }
    }

    private String extractUuid(JsonObject json) {
        String uuid = getJsonString(json, "id");
        if (uuid.isEmpty()) {
            uuid = getJsonString(json, "uuid");
        }
        try {
            return toUndashedUuid(UUIDUtils.fromString(uuid));
        } catch (Exception ignored) {
            return "";
        }
    }

    private String getJsonString(JsonObject json, String field) {
        if (json == null || !json.has(field) || json.get(field).isJsonNull()) {
            return "";
        }
        return json.get(field).getAsString().trim();
    }

    private String toUndashedUuid(UUID uuid) {
        return uuid == null ? "" : uuid.toString().replace("-", "");
    }

    protected HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    private HttpResult executeGetRequest(URL url) throws IOException {
        HttpURLConnection connection = openConnection(url);
        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty(
                "User-Agent",
                Mellow.NAME + "/" + Mellow.VERSION
            );
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        connection.getInputStream(),
                        StandardCharsets.UTF_8
                    )
                );
                try {
                    return new HttpResult(
                        responseCode,
                        reader.lines().collect(Collectors.joining())
                    );
                } finally {
                    reader.close();
                }
            }
            return new HttpResult(responseCode, "");
        } finally {
            connection.disconnect();
        }
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

    private static final class HttpResult {

        private final int statusCode;
        private final String body;

        private HttpResult(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }
    }

    public static final class MojangProfile {

        private final String name;
        private final UUID uuid;

        private MojangProfile(String name, UUID uuid) {
            this.name = name;
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public UUID getUuid() {
            return uuid;
        }
    }
}
