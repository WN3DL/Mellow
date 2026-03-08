package com.roxiun.mellow.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StatusCommand extends CommandBase {

    private final MojangApi mojangApi;
    private final MellowOneConfig config;
    private final OkHttpClient client = new OkHttpClient();

    public StatusCommand(MojangApi mojangApi, MellowOneConfig config) {
        this.mojangApi = mojangApi;
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "status";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/status <username>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(sender, "§cInvalid usage! Use /status <username>");
            return;
        }

        boolean hasHypixelKey = hasValue(config.hypixelApiKey);
        boolean hasLunaKey = hasValue(config.lunaPingApiKey);
        if (!hasHypixelKey && !hasLunaKey) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cSet a Hypixel API key or Luna API key in OneConfig first."
            );
            return;
        }

        String username = args[0];
        ChatUtils.sendCommandMessage(
            sender,
            "§7Fetching status for §f" + username + "§7..."
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

            List<String> lines = new ArrayList<>();
            lines.add("§f" + username);

            if (hasHypixelKey) {
                addHypixelStatus(lines, uuid);
                addHypixelLastLogin(lines, uuid);
            }
            if (hasLunaKey) {
                addLunaLobbyMessage(lines, uuid);
            }

            if (lines.size() == 1) {
                lines.add("§7No data available.");
            }

            MainThreadDispatcher.run(() ->
                ChatUtils.sendMultilineCommandMessage(sender, lines)
            );
        });
    }

    private void addHypixelStatus(List<String> lines, UUID uuid) {
        try {
            String url =
                "https://api.hypixel.net/status?key=" +
                config.hypixelApiKey +
                "&uuid=" +
                uuid;
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mellow/" + Mellow.VERSION)
                .build();

            try (Response response = client.newCall(request).execute()) {
                ResponseBody body = response.body();
                if (body == null) {
                    return;
                }

                JsonObject json = new JsonParser()
                    .parse(body.string())
                    .getAsJsonObject();
                if (!isSuccess(json) || !json.has("session")) {
                    return;
                }

                JsonObject session = json.getAsJsonObject("session");
                boolean online =
                    session.has("online") && session.get("online").getAsBoolean();
                if (!online) {
                    lines.add("§rStatus: §7Hidden / Offline");
                    return;
                }

                StringBuilder status = new StringBuilder("§aOnline");
                String gameType = getString(session, "gameType");
                String mode = getString(session, "mode");
                if (gameType != null) {
                    status.append(" §7- §f").append(formatGameType(gameType));
                    if (mode != null && !"LOBBY".equalsIgnoreCase(mode)) {
                        status.append(" §7(").append(mode).append("§7)");
                    }
                }
                lines.add("§rStatus: " + status);
            }
        } catch (Exception ignored) {}
    }

    private void addHypixelLastLogin(List<String> lines, UUID uuid) {
        try {
            String url =
                "https://api.hypixel.net/player?key=" +
                config.hypixelApiKey +
                "&uuid=" +
                uuid;
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mellow/" + Mellow.VERSION)
                .build();

            try (Response response = client.newCall(request).execute()) {
                ResponseBody body = response.body();
                if (body == null) {
                    return;
                }

                JsonObject json = new JsonParser()
                    .parse(body.string())
                    .getAsJsonObject();
                if (!isSuccess(json) || !json.has("player")) {
                    return;
                }

                JsonObject player = json.getAsJsonObject("player");
                if (player.has("lastLogin") && !player.get("lastLogin").isJsonNull()) {
                    lines.add(
                        "§7Last login: §f" +
                        formatDateTime(player.get("lastLogin").getAsLong())
                    );
                }
            }
        } catch (Exception ignored) {}
    }

    private void addLunaLobbyMessage(List<String> lines, UUID uuid) {
        try {
            String apiKey = config.lunaPingApiKey.trim();
            String authKey = "LunaAPI-" + apiKey;
            String url = "https://lunaaaa.net/chat/" + uuid + "?key=" + authKey;
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mellow/" + Mellow.VERSION)
                .header("Authorization", "Bearer " + authKey)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return;
                }

                ResponseBody body = response.body();
                if (body == null) {
                    return;
                }

                JsonObject json = new JsonParser()
                    .parse(body.string())
                    .getAsJsonObject();
                if (!json.has("history") || !json.get("history").isJsonArray()) {
                    return;
                }

                JsonArray history = json.getAsJsonArray("history");
                JsonObject entry = firstHistoryEntry(history);
                if (entry == null) {
                    return;
                }

                String message = getFirstString(
                    entry,
                    "message",
                    "content",
                    "msg",
                    "text"
                );
                if (message == null || message.isEmpty()) {
                    return;
                }

                long timestamp = getFirstTimestamp(
                    entry,
                    "timestamp",
                    "time",
                    "date",
                    "sent_at",
                    "created_at"
                );
                String line = "§7Last lobby msg: §f\"" + message + "\"";
                if (timestamp > 0L) {
                    line += " §7(" + formatRelativeTime(timestamp) + ")";
                }
                lines.add(line);
            }
        } catch (Exception ignored) {}
    }

    private JsonObject firstHistoryEntry(JsonArray history) {
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).isJsonObject()) {
                return history.get(i).getAsJsonObject();
            }
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            if (history.get(i).isJsonObject()) {
                return history.get(i).getAsJsonObject();
            }
        }
        return null;
    }

    private String getFirstString(JsonObject object, String... fields) {
        for (String field : fields) {
            String value = getString(object, field);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private long getFirstTimestamp(JsonObject object, String... fields) {
        for (String field : fields) {
            if (!object.has(field) || object.get(field).isJsonNull()) {
                continue;
            }

            try {
                double numeric = object.get(field).getAsDouble();
                long timestamp = (long) numeric;
                if (timestamp > 1_000_000_000_000L) {
                    timestamp /= 1000L;
                }
                if (timestamp > 0L) {
                    return timestamp;
                }
            } catch (Exception ignored) {}

            try {
                String iso = object.get(field).getAsString();
                Date parsed = null;
                try {
                    parsed = isoFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(iso);
                } catch (Exception ignored) {}
                if (parsed == null) {
                    try {
                        parsed = isoFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(iso);
                    } catch (Exception ignored) {}
                }
                if (parsed != null) {
                    return parsed.getTime() / 1000L;
                }
            } catch (Exception ignored) {}
        }
        return -1L;
    }

    private boolean isSuccess(JsonObject json) {
        return json.has("success") && json.get("success").getAsBoolean();
    }

    private boolean hasValue(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String getString(JsonObject json, String key) {
        if (!json.has(key) || json.get(key).isJsonNull()) {
            return null;
        }
        return json.get(key).getAsString();
    }

    private String formatGameType(String gameType) {
        String normalized = gameType.toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = normalized.split(" ");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }

    private String formatDateTime(long epochMillis) {
        SimpleDateFormat format = isoFormat("yyyy-MM-dd HH:mm:ss 'UTC'");
        return format.format(new Date(epochMillis));
    }

    private String formatRelativeTime(long unixSeconds) {
        long now = System.currentTimeMillis() / 1000L;
        long diff = Math.max(0L, now - unixSeconds);

        if (diff < 60L) {
            return diff + "s ago";
        }
        if (diff < 3600L) {
            return (diff / 60L) + "m ago";
        }
        if (diff < 86_400L) {
            return (diff / 3600L) + "h ago";
        }
        return (diff / 86_400L) + "d ago";
    }

    private SimpleDateFormat isoFormat(String pattern) {
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ROOT);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
