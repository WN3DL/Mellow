package com.roxiun.mellow.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.cache.TimedValueCache;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NameHistoryCommand extends CommandBase {

    private static final long HISTORY_CACHE_TTL_MS = 1_800_000L;
    private static final Pattern NAMEMC_NAME_PATTERN = Pattern.compile(
        "<a[^>]+href=[\"']/search\\?q=([^\"']+)[\"'][^>]*>([^<]+)</a>"
    );
    private static final Pattern NAMEMC_DATE_PATTERN = Pattern.compile(
        "<time[^>]+datetime=[\"']([^\"']+)[\"']"
    );

    private final MojangApi mojangApi;
    private final OkHttpClient client;
    private final TimedValueCache<String, List<NameEntry>> historyCache =
        new TimedValueCache<>(HISTORY_CACHE_TTL_MS);

    public NameHistoryCommand(MojangApi mojangApi) {
        this(mojangApi, new OkHttpClient());
    }

    NameHistoryCommand(MojangApi mojangApi, OkHttpClient client) {
        this.mojangApi = mojangApi;
        this.client = client == null ? new OkHttpClient() : client;
    }

    @Override
    public String getCommandName() {
        return "namehistory";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("nameh", "names", "nh");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/namehistory <username>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use /namehistory <username>"
            );
            return;
        }

        String username = args[0];
        ChatUtils.sendCommandMessage(
            sender,
            "§7Fetching name history for §f" + username + "§7..."
        );

        AsyncExecutor.getInstance().command(() -> {
            UUID uuid = PlayerUtils.resolveLookupUuid(username, mojangApi);
            if (uuid == null) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cPlayer not found: §r" + username
                    )
                );
                return;
            }

            List<NameEntry> entries = getMergedHistory(uuid);

            List<String> lines = new ArrayList<>();
            lines.add("§f" + username + " §7- Name History");

            if (entries.isEmpty()) {
                lines.add("§7No name history found.");
            } else {
                for (NameEntry entry : entries) {
                    if (entry.date != null && !entry.date.isEmpty()) {
                        lines.add(
                            "§f" + entry.name + " §7(since " + formatDate(entry.date) + ")"
                        );
                    } else {
                        lines.add("§f" + entry.name + " §7(original)");
                    }
                }
            }

            MainThreadDispatcher.run(() ->
                ChatUtils.sendMultilineCommandMessage(sender, lines)
            );
        });
    }

    List<NameEntry> getMergedHistory(UUID uuid) {
        if (uuid == null) {
            return new ArrayList<>();
        }

        String cacheKey = uuid.toString();
        if (historyCache.containsFresh(cacheKey)) {
            return copyEntries(historyCache.get(cacheKey));
        }

        Map<String, NameEntry> merged = new LinkedHashMap<>();
        addEntries(merged, fetchAshconHistory(cacheKey));
        addEntries(merged, fetchLabyHistory(cacheKey.replace("-", "")));
        addEntries(merged, fetchNameMCHistory(cacheKey));

        List<NameEntry> entries = new ArrayList<>(merged.values());
        historyCache.put(cacheKey, copyEntries(entries));
        return entries;
    }

    private void addEntries(Map<String, NameEntry> map, List<NameEntry> entries) {
        if (entries == null) {
            return;
        }
        for (NameEntry entry : entries) {
            if (entry == null || entry.name == null || entry.name.isEmpty()) {
                continue;
            }
            String key = entry.name.toLowerCase(Locale.ROOT);
            NameEntry existing = map.get(key);
            if (existing == null || (existing.date == null && entry.date != null)) {
                map.put(key, entry);
            }
        }
    }

    private List<NameEntry> fetchAshconHistory(String uuid) {
        try {
            Request request = new Request.Builder()
                .url("https://api.ashcon.app/mojang/v2/user/" + uuid)
                .header("User-Agent", "Mellow/" + Mellow.VERSION)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    return null;
                }

                JsonObject json = new JsonParser()
                    .parse(body.string())
                    .getAsJsonObject();
                if (!json.has("username_history")) {
                    return null;
                }

                JsonArray history = json.getAsJsonArray("username_history");
                List<NameEntry> entries = new ArrayList<>();
                for (int i = history.size() - 1; i >= 0; i--) {
                    JsonObject entry = history.get(i).getAsJsonObject();
                    String name = entry.get("username").getAsString();
                    String changedAt = entry.has("changed_at") &&
                        !entry.get("changed_at").isJsonNull()
                        ? entry.get("changed_at").getAsString()
                        : null;
                    entries.add(new NameEntry(name, changedAt));
                }
                return entries;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<NameEntry> fetchLabyHistory(String compactUuid) {
        try {
            Request request = new Request.Builder()
                .url("https://laby.net/api/v3/user/" + compactUuid + "/profile")
                .header("User-Agent", "Mellow/" + Mellow.VERSION)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    return null;
                }

                JsonObject json = new JsonParser()
                    .parse(body.string())
                    .getAsJsonObject();
                JsonArray history = null;
                if (json.has("name_history")) {
                    history = json.getAsJsonArray("name_history");
                } else if (json.has("username_history")) {
                    history = json.getAsJsonArray("username_history");
                }
                if (history == null) {
                    return null;
                }

                List<NameEntry> entries = new ArrayList<>();
                for (int i = history.size() - 1; i >= 0; i--) {
                    JsonElement element = history.get(i);
                    if (!element.isJsonObject()) {
                        continue;
                    }

                    JsonObject entry = element.getAsJsonObject();
                    String name = null;
                    if (entry.has("username")) {
                        name = entry.get("username").getAsString();
                    } else if (entry.has("name")) {
                        name = entry.get("name").getAsString();
                    }
                    if (name == null || name.isEmpty()) {
                        continue;
                    }

                    String changedAt = null;
                    if (entry.has("timestamp") && !entry.get("timestamp").isJsonNull()) {
                        long timestamp = entry.get("timestamp").getAsLong();
                        if (timestamp > 1_000_000_000_000L) {
                            timestamp /= 1000L;
                        }
                        changedAt = isoFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
                            .format(new Date(timestamp * 1000L));
                    } else if (entry.has("changed_at") && !entry.get("changed_at").isJsonNull()) {
                        changedAt = entry.get("changed_at").getAsString();
                    }

                    entries.add(new NameEntry(name, changedAt));
                }
                return entries;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private List<NameEntry> fetchNameMCHistory(String uuid) {
        try {
            Request request = new Request.Builder()
                .url("https://namemc.com/profile/" + uuid)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml")
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    return null;
                }

                String html = body.string();
                int start = html.indexOf("id=\"minecraft-names\"");
                if (start < 0) {
                    start = html.indexOf("id=\"names\"");
                }
                if (start < 0) {
                    start = html.indexOf("Name History");
                }
                if (start < 0) {
                    return null;
                }

                String section = html.substring(start, Math.min(start + 8000, html.length()));
                List<String> dates = new ArrayList<>();
                Matcher dateMatcher = NAMEMC_DATE_PATTERN.matcher(section);
                while (dateMatcher.find()) {
                    dates.add(dateMatcher.group(1));
                }

                List<String> names = new ArrayList<>();
                Matcher nameMatcher = NAMEMC_NAME_PATTERN.matcher(section);
                while (nameMatcher.find()) {
                    String rawName = nameMatcher.group(2).trim();
                    if (rawName.isEmpty()) {
                        continue;
                    }
                    names.add(URLDecoder.decode(rawName, StandardCharsets.UTF_8.name()));
                }

                if (names.isEmpty()) {
                    return null;
                }

                List<NameEntry> entries = new ArrayList<>();
                for (int i = 0; i < names.size(); i++) {
                    String date = i < dates.size() ? dates.get(i) : null;
                    entries.add(new NameEntry(names.get(i), date));
                }
                return entries;
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private String formatDate(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        String[] patterns = {
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX",
            "yyyy-MM-dd",
        };
        for (String pattern : patterns) {
            try {
                Date parsed = isoFormat(pattern).parse(value);
                return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(parsed);
            } catch (Exception ignored) {}
        }
        return value;
    }

    private SimpleDateFormat isoFormat(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    private List<NameEntry> copyEntries(List<NameEntry> entries) {
        return entries == null ? new ArrayList<>() : new ArrayList<>(entries);
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    private static final class NameEntry {

        private final String name;
        private final String date;

        private NameEntry(String name, String date) {
            this.name = name;
            this.date = date;
        }
    }
}
