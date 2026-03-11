package com.roxiun.mellow.api.seraph;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.util.cache.TimedValueCache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SeraphApi {

    private static final long CLIENT_CACHE_TTL_MS = 300_000L;
    private static final long TAG_CACHE_TTL_MS = 120_000L;

    private final MojangApi mojangApi;
    private final TimedValueCache<String, SeraphClientType> clientTypeCache =
        new TimedValueCache<>(CLIENT_CACHE_TTL_MS);
    private final TimedValueCache<String, List<SeraphTag>> tagCache =
        new TimedValueCache<>(TAG_CACHE_TTL_MS);

    public SeraphApi(MojangApi mojangApi) {
        this.mojangApi = mojangApi;
    }

    public SeraphClientType fetchClientType(String uuid, String seraphApiKey) {
        String cacheKey = buildCacheKey(uuid, seraphApiKey);
        if (!cacheKey.isEmpty() && clientTypeCache.containsFresh(cacheKey)) {
            return clientTypeCache.get(cacheKey);
        }

        SeraphClientType clientType = null;
        boolean shouldCache = false;
        try {
            if (
                uuid == null ||
                uuid.equals("ERROR") ||
                uuid.isEmpty() ||
                seraphApiKey == null ||
                seraphApiKey.isEmpty()
            ) {
                return null;
            }

            String apiUrl =
                "https://api.seraph.si/private-access/client?key=" +
                seraphApiKey +
                "&id=" +
                uuid;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                clientType = null;
            } else {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );
                String response = in.lines().collect(Collectors.joining());
                in.close();
                clientType = parseClientTag(response);
                shouldCache = true;
            }
        } catch (Exception ignored) {
            clientType = null;
        }

        if (shouldCache && !cacheKey.isEmpty()) {
            clientTypeCache.put(cacheKey, clientType);
        }
        return clientType;
    }

    public List<SeraphTag> fetchSeraphTags(String uuid, String seraphApiKey)
        throws IOException {
        String cacheKey = buildCacheKey(uuid, seraphApiKey);
        if (!cacheKey.isEmpty() && tagCache.containsFresh(cacheKey)) {
            return copyTags(tagCache.get(cacheKey));
        }

        List<SeraphTag> tags = new ArrayList<>();
        boolean shouldCache = false;
        try {
            // If the UUID is invalid for any reason, throw an exception
            if (uuid == null || uuid.equals("ERROR") || uuid.isEmpty()) {
                throw new IOException("Invalid UUID provided.");
            }

            String apiUrl = "https://api.seraph.si/cubelify/blacklist/" + uuid;
            if (seraphApiKey != null && !seraphApiKey.isEmpty()) {
                apiUrl += "?key=" + seraphApiKey;
            }

            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
                );
                String response = in.lines().collect(Collectors.joining());
                in.close();
                tags = parseTags(response);
                shouldCache = true;
            } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                tags = new ArrayList<>(); // Player has no tags
                shouldCache = true;
            } else {
                throw new IOException(
                    "Seraph API request failed with code: " + responseCode
                );
            }
        } catch (IOException e) {
            tags = new ArrayList<>();
        }

        if (shouldCache && !cacheKey.isEmpty()) {
            tagCache.put(cacheKey, copyTags(tags));
        }
        return copyTags(tags);
    }

    private List<SeraphTag> parseTags(String response) {
        try {
            JsonObject json = new JsonParser()
                .parse(response)
                .getAsJsonObject();
            if (json.has("tags")) {
                JsonArray tagsArray = json.getAsJsonArray("tags");
                if (tagsArray.size() > 0) {
                    List<SeraphTag> tags = new ArrayList<>();
                    for (JsonElement tagElement : tagsArray) {
                        JsonObject tagObj = tagElement.getAsJsonObject();

                        String icon = tagObj.has("icon")
                            ? tagObj.get("icon").getAsString()
                            : "";
                        String tooltip = tagObj.has("tooltip")
                            ? tagObj.get("tooltip").getAsString()
                            : "";
                        int color = tagObj.has("color")
                            ? tagObj.get("color").getAsInt()
                            : 0;
                        String tagName = tagObj.has("tag_name")
                            ? tagObj.get("tag_name").getAsString()
                            : "";
                        String text = tagObj.has("text")
                            ? tagObj.get("text").getAsString()
                            : null;
                        int textColor = tagObj.has("textColor")
                            ? tagObj.get("textColor").getAsInt()
                            : 0;

                        // Skip seraph.advertisement tags
                        if (
                            "seraph.advertisement".equals(tagName)
                        ) {
                            continue;
                        }

                        tags.add(
                            new SeraphTag(
                                icon,
                                tooltip,
                                color,
                                tagName,
                                text,
                                textColor
                            )
                        );
                    }
                    return tags;
                }
            }
        } catch (Exception e) {
            // If parsing fails, return empty list
        }
        return new ArrayList<>();
    }

    private SeraphClientType parseClientTag(String response) {
        if (response == null || response.isEmpty()) {
            return null;
        }

        SeraphClientType clientType = parseClientTypeFromJson(response);
        if (clientType != null) {
            return clientType;
        }

        String lower = response.toLowerCase();
        int statusIndex = lower.indexOf("currently on ");
        if (statusIndex < 0) {
            return null;
        }

        int start = statusIndex + "currently on ".length();
        int clientIndex = lower.indexOf(" client", start);
        if (clientIndex < 0) {
            return null;
        }

        String clientName = response.substring(start, clientIndex).trim();
        if (clientName.isEmpty()) {
            return null;
        }
        return SeraphClientType.fromDetectedName(clientName);
    }

    private SeraphClientType parseClientTypeFromJson(String response) {
        try {
            JsonObject json = new JsonParser().parse(response).getAsJsonObject();
            if (!json.has("tags") || !json.get("tags").isJsonArray()) {
                return null;
            }

            JsonArray tags = json.getAsJsonArray("tags");
            for (JsonElement element : tags) {
                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject tag = element.getAsJsonObject();
                if (tag.has("tag_name") && !tag.get("tag_name").isJsonNull()) {
                    SeraphClientType clientType = SeraphClientType.fromDetectedName(
                        tag.get("tag_name").getAsString()
                    );
                    if (clientType != null) {
                        return clientType;
                    }
                }

                if (tag.has("tooltip") && !tag.get("tooltip").isJsonNull()) {
                    SeraphClientType clientType = parseClientTag(
                        tag.get("tooltip").getAsString()
                    );
                    if (clientType != null) {
                        return clientType;
                    }
                }
            }
        } catch (Exception ignored) {}

        return null;
    }

    private String buildCacheKey(String uuid, String seraphApiKey) {
        if (uuid == null || uuid.trim().isEmpty() || "ERROR".equals(uuid)) {
            return "";
        }

        return (
            uuid.trim().toLowerCase(Locale.ROOT) +
            "|" +
            normalizeApiKey(seraphApiKey)
        );
    }

    private String normalizeApiKey(String apiKey) {
        return apiKey == null ? "" : apiKey.trim();
    }

    public void clearCache() {
        clientTypeCache.clear();
        tagCache.clear();
    }

    public void clearPlayer(String uuid) {
        String normalizedUuid = normalizeUuid(uuid);
        if (normalizedUuid.isEmpty()) {
            return;
        }

        clientTypeCache.removeMatching(key ->
            key != null && key.startsWith(normalizedUuid + "|")
        );
        tagCache.removeMatching(key ->
            key != null && key.startsWith(normalizedUuid + "|")
        );
    }

    private String normalizeUuid(String uuid) {
        if (uuid == null) {
            return "";
        }

        String normalized = uuid.trim().toLowerCase(Locale.ROOT);
        return "error".equals(normalized) ? "" : normalized;
    }

    private List<SeraphTag> copyTags(List<SeraphTag> tags) {
        return tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }
}
