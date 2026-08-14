package com.roxiun.mellow.api.coral;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.util.cache.TimedValueCache;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class CoralApi {

    private static final String PLAYER_TAGS_ENDPOINT =
        "https://api.urchin.gg/v3/player/tags";
    private static final long TAG_CACHE_TTL_MS = 120_000L;

    private final TimedValueCache<String, List<CoralTag>> tagCache =
        new TimedValueCache<>(TAG_CACHE_TTL_MS);

    public List<CoralTag> fetchCoralTags(
        String uuid,
        String playerName,
        String coralKey
    ) throws IOException {
        String apiKey = normalizeApiKey(coralKey);
        if (apiKey.isEmpty()) {
            throw new IOException("A Coral API key is required.");
        }

        String identifier = selectIdentifier(uuid, playerName);
        if (identifier.isEmpty()) {
            throw new IOException("A player UUID or username is required.");
        }

        String cacheKey = buildTagCacheKey(identifier, apiKey);
        if (tagCache.containsFresh(cacheKey)) {
            return copyTags(tagCache.get(cacheKey));
        }

        URL url = new URL(
            PLAYER_TAGS_ENDPOINT +
            "?player=" +
            URLEncoder.encode(identifier, StandardCharsets.UTF_8.name())
        );
        HttpURLConnection connection = openConnection(url);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("X-API-Key", apiKey);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty(
            "User-Agent",
            Mellow.NAME + "/" + Mellow.VERSION
        );
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        List<CoralTag> tags;
        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw buildHttpException(connection, responseCode);
            }

            try (InputStream input = connection.getInputStream()) {
                tags = parseTags(readBody(input));
            }
        } finally {
            connection.disconnect();
        }
        tagCache.put(cacheKey, copyTags(tags));
        return copyTags(tags);
    }

    protected HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    private List<CoralTag> parseTags(String response) throws IOException {
        try {
            JsonObject json = new JsonParser()
                .parse(response)
                .getAsJsonObject();
            JsonArray tagsArray = json.getAsJsonArray("tags");
            if (tagsArray == null) {
                throw new IOException("Coral response did not contain tags.");
            }

            List<CoralTag> tags = new ArrayList<>();
            for (JsonElement tagElement : tagsArray) {
                JsonObject tag = tagElement.getAsJsonObject();
                String tagType = tag.get("tag_type").getAsString();
                String reason = tag.get("reason").getAsString();
                long addedOn = tag.get("added_on").getAsLong();
                boolean hideUsername = tag.get("hide_username").getAsBoolean();
                tags.add(
                    new CoralTag(
                        tagType,
                        reason,
                        addedOn,
                        hideUsername,
                        getNullableLong(tag, "added_by"),
                        getNullableString(tag, "added_by_username"),
                        getNullableLong(tag, "expires_at")
                    )
                );
            }
            return tags;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Unable to parse Coral tag response.", e);
        }
    }

    private Long getNullableLong(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull() ? null : value.getAsLong();
    }

    private String getNullableString(JsonObject object, String field) {
        JsonElement value = object.get(field);
        return value == null || value.isJsonNull()
            ? null
            : value.getAsString();
    }

    private IOException buildHttpException(
        HttpURLConnection connection,
        int responseCode
    ) {
        String detail = "";
        InputStream errorStream = connection.getErrorStream();
        if (errorStream != null) {
            try (InputStream input = errorStream) {
                JsonObject error = new JsonParser()
                    .parse(readBody(input))
                    .getAsJsonObject();
                if (error.has("error")) {
                    detail = ": " + error.get("error").getAsString();
                }
            } catch (Exception ignored) {}
        }
        return new IOException(
            "Coral API request failed with response code " +
            responseCode +
            detail
        );
    }

    private String readBody(InputStream input) throws IOException {
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8)
            )
        ) {
            return reader.lines().collect(Collectors.joining());
        }
    }

    public void clearCache() {
        tagCache.clear();
    }

    public void clearPlayer(String uuid, String playerName) {
        String normalizedUuid = normalizeIdentifier(uuid);
        String normalizedName = normalizeIdentifier(playerName);
        tagCache.removeMatching(key ->
            matchesCachePrefix(key, normalizedUuid) ||
            matchesCachePrefix(key, normalizedName)
        );
    }

    private String selectIdentifier(String uuid, String playerName) {
        String identifier = normalizeIdentifier(uuid);
        if (identifier.isEmpty() || "error".equals(identifier)) {
            identifier = normalizeIdentifier(playerName);
        }
        return identifier;
    }

    private String buildTagCacheKey(String identifier, String apiKey) {
        return normalizeIdentifier(identifier) + "|" + apiKey;
    }

    private String normalizeApiKey(String apiKey) {
        return apiKey == null ? "" : apiKey.trim();
    }

    private String normalizeIdentifier(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean matchesCachePrefix(String key, String prefix) {
        return (
            key != null &&
            prefix != null &&
            !prefix.isEmpty() &&
            key.startsWith(prefix + "|")
        );
    }

    private List<CoralTag> copyTags(List<CoralTag> tags) {
        return tags == null ? new ArrayList<>() : new ArrayList<>(tags);
    }
}
