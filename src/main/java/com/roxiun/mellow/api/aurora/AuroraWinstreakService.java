package com.roxiun.mellow.api.aurora;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class AuroraWinstreakService {

    private static final String WINSTREAK_URL =
        "https://bordic.xyz/api/v2/resources/winstreak";

    private final OkHttpClient client = new OkHttpClient();
    private final Map<String, Integer> winstreakCache = new ConcurrentHashMap<>();
    private final Set<String> fetchInProgress = ConcurrentHashMap.newKeySet();
    private final AtomicBoolean errorShownThisSession = new AtomicBoolean(false);

    public int getCachedWinstreak(String compactUuid) {
        Integer cached = winstreakCache.get(compactUuid);
        return cached == null ? -1 : cached;
    }

    public boolean hasCachedWinstreak(String compactUuid) {
        return winstreakCache.containsKey(compactUuid);
    }

    public boolean tryStartFetch(String compactUuid) {
        return fetchInProgress.add(compactUuid);
    }

    public void finishFetch(String compactUuid) {
        fetchInProgress.remove(compactUuid);
    }

    public void storeInCache(String compactUuid, int winstreak) {
        winstreakCache.put(compactUuid, winstreak);
    }

    public void clearPlayer(String compactUuid) {
        if (compactUuid == null || compactUuid.isEmpty()) {
            return;
        }

        winstreakCache.remove(compactUuid);
        fetchInProgress.remove(compactUuid);
    }

    public void clearCache() {
        winstreakCache.clear();
        fetchInProgress.clear();
    }

    public boolean hasShownError() {
        return errorShownThisSession.get();
    }

    public void markErrorShown() {
        errorShownThisSession.set(true);
    }

    public int fetchWinstreakBlocking(String compactUuid, String apiKey)
        throws IOException {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return -1;
        }

        String url = WINSTREAK_URL + "?key=" + apiKey.trim() + "&uuid=" + compactUuid;
        Request request = new Request.Builder()
            .url(url)
            .header("User-Agent", "Mellow/" + Mellow.VERSION)
            .build();

        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body == null) {
                return -1;
            }

            String bodyString = body.string();
            JsonObject json = new JsonParser()
                .parse(bodyString)
                .getAsJsonObject();

            if (json.has("message") && !json.get("message").isJsonNull()) {
                String message = json.get("message").getAsString();
                if (
                    message.contains("Invalid") ||
                    message.contains("Unauthorized")
                ) {
                    throw new IOException(message);
                }
            }

            int winstreak = extractWinstreak(json);
            if (winstreak >= 0) {
                return winstreak;
            }

            if (!response.isSuccessful()) {
                throw new IOException("HTTP " + response.code());
            }

            String preview = bodyString.length() > 300
                ? bodyString.substring(0, 300) + "..."
                : bodyString;
            throw new IOException("No winstreak field found. Response: " + preview);
        }
    }

    private int extractWinstreak(JsonObject json) {
        for (String key : new String[] {
            "winstreak",
            "current_winstreak",
            "ws",
            "win_streak",
        }) {
            if (json.has(key) && !json.get(key).isJsonNull()) {
                return json.get(key).getAsInt();
            }
        }

        if (!json.has("data") || !json.get("data").isJsonObject()) {
            return -1;
        }

        JsonObject data = json.getAsJsonObject("data");
        for (String key : new String[] {
            "winstreak",
            "current_winstreak",
            "ws",
            "win_streak",
        }) {
            if (data.has(key) && !data.get(key).isJsonNull()) {
                return data.get(key).getAsInt();
            }
        }

        return -1;
    }
}
