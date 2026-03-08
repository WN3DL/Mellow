package com.roxiun.mellow.api.aurora;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.roxiun.mellow.util.cache.TimedValueCache;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AuroraApi {

    private static final long QUERY_CACHE_TTL_MS = 60_000L;

    private final OkHttpClient client;
    private final Gson gson;
    private final TimedValueCache<String, String> queryCache =
        new TimedValueCache<>(QUERY_CACHE_TTL_MS);
    private static final String BASE_URL =
        "https://bordic.xyz/api/v2/resources/lookup/";

    public AuroraApi() {
        this(new OkHttpClient(), new Gson());
    }

    AuroraApi(OkHttpClient client) {
        this(client, new Gson());
    }

    AuroraApi(OkHttpClient client, Gson gson) {
        this.client = client == null ? new OkHttpClient() : client;
        this.gson = gson == null ? new Gson() : gson;
    }

    public AuroraResponse queryStats(
        String type,
        String value,
        int range,
        int max,
        String apiKey
    ) throws IOException {
        if (apiKey == null || apiKey.isEmpty()) {
            return null;
        }

        String cacheKey = buildCacheKey(type, value, range, max, apiKey);
        if (queryCache.containsFresh(cacheKey)) {
            return parseResponse(queryCache.get(cacheKey));
        }

        String url =
            BASE_URL +
            type +
            "?key=" +
            apiKey +
            "&value=" +
            value +
            "&range=" +
            range +
            "&max=" +
            max;

        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mellow/4.1.0")
            .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                // Log the error or handle it more gracefully
                System.err.println("Aurora API request failed: " + response);
                queryCache.put(cacheKey, null);
                return null;
            }

            ResponseBody body = response.body();
            if (body == null) {
                queryCache.put(cacheKey, null);
                return null;
            }

            String payload = body.string();
            queryCache.put(cacheKey, payload);
            return parseResponse(payload);
        }
    }

    public void clearCache() {
        queryCache.clear();
    }

    private AuroraResponse parseResponse(String payload) {
        return payload == null ? null : gson.fromJson(payload, AuroraResponse.class);
    }

    private String buildCacheKey(
        String type,
        String value,
        int range,
        int max,
        String apiKey
    ) {
        return (
            safe(type).toLowerCase(Locale.ROOT) +
            "|" +
            safe(value).toLowerCase(Locale.ROOT) +
            "|" +
            range +
            "|" +
            max +
            "|" +
            safe(apiKey)
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static class AuroraResponse {

        @SerializedName("success")
        public boolean success;

        @SerializedName("data")
        public List<PlayerMatch> data;
    }

    public static class PlayerMatch {

        @SerializedName("name")
        public String name;

        @SerializedName("distance")
        public int distance;
    }
}
