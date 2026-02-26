package com.roxiun.mellow.api.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HypixelApiUtils {

    public static String fetchPlayerData(String urlString, String userAgent) {
        return fetchPlayerData(urlString, userAgent, Collections.emptyMap());
    }

    public static String fetchPlayerData(
        String urlString,
        String userAgent,
        Map<String, String> headers
    ) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            if (userAgent != null) {
                connection.setRequestProperty("User-Agent", userAgent);
            }
            connection.setRequestProperty("Accept", "application/json");

            for (Map.Entry<String, String> entry : headers.entrySet()) {
                connection.setRequestProperty(entry.getKey(), entry.getValue());
            }

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return "";
            }

            BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            if (urlString.contains("nadeshiko")) {
                Pattern pattern = Pattern.compile(
                    "playerData = JSON.parse\\(decodeURIComponent\\(\\\"(.*?)\\\"\\)\\)"
                );
                Matcher matcher = pattern.matcher(response.toString());

                if (matcher.find()) {
                    String playerDataEncoded = matcher.group(1);
                    return URLDecoder.decode(playerDataEncoded, "UTF-8");
                }
            }

            return response.toString();
        } catch (Exception e) {
            return "";
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public static BedwarsPlayer parsePlayerData(String json, String provider) {
        ProviderId providerId;
        if ("Abyss".equalsIgnoreCase(provider)) {
            providerId = ProviderId.ABYSS;
        } else if ("Nadeshiko".equalsIgnoreCase(provider)) {
            providerId = ProviderId.NADESHIKO;
        } else {
            providerId = ProviderId.HYPIXEL_PUBLIC;
        }
        return parsePlayerData(json, providerId);
    }

    public static BedwarsPlayer parsePlayerData(String json, ProviderId providerId) {
        try {
            JsonObject rootObject = new JsonParser().parse(json).getAsJsonObject();
            JsonObject playerObject = getPlayerObject(rootObject, providerId);
            if (playerObject == null) {
                return null;
            }

            String name = getString(
                playerObject,
                "displayname",
                getString(rootObject, "name", "[]")
            );
            if (providerId == ProviderId.NADESHIKO) {
                JsonObject profile = getObject(rootObject, "profile");
                name =
                    getString(
                        profile,
                        "hypixel_displayname",
                        getString(rootObject, "name", name)
                    );
            }
            String formattedName = getFormattedNameWithRank(
                rootObject,
                playerObject,
                providerId,
                name
            );

            JsonObject achievements = getObject(playerObject, "achievements");
            int stars = getInt(achievements, "bedwars_level", 0);

            JsonObject stats = getObject(playerObject, "stats");
            JsonObject bedwarsStats = getObject(stats, "Bedwars");

            int finalKills = getInt(bedwarsStats, "final_kills_bedwars", 0);
            int finalDeaths = getInt(bedwarsStats, "final_deaths_bedwars", 0);
            double fkdr = finalDeaths == 0
                ? finalKills
                : (double) finalKills / finalDeaths;

            int winstreak = getInt(bedwarsStats, "winstreak", 0);
            int wins = getInt(bedwarsStats, "wins_bedwars", 0);
            int losses = getInt(bedwarsStats, "losses_bedwars", 0);
            int bedsBroken = getInt(bedwarsStats, "beds_broken_bedwars", 0);
            int bedsLost = getInt(bedwarsStats, "beds_lost_bedwars", 0);
            int finals = finalKills;

            return new BedwarsPlayer(
                name,
                formattedName,
                FormattingUtils.formatStars(String.valueOf(stars)),
                fkdr,
                winstreak,
                finalKills,
                finalDeaths,
                wins,
                losses,
                bedsBroken,
                bedsLost,
                finals
            );
        } catch (Exception e) {
            return null;
        }
    }

    private static String getFormattedNameWithRank(
        JsonObject rootObject,
        JsonObject playerObject,
        ProviderId providerId,
        String plainName
    ) {
        if (plainName == null || plainName.isEmpty() || "[]".equals(plainName)) {
            plainName = getString(rootObject, "name", "Unknown");
        }

        if (providerId == ProviderId.NADESHIKO) {
            JsonObject profile = getObject(rootObject, "profile");
            String tagged = getString(profile, "tagged_name", "");
            if (!tagged.isEmpty()) {
                return tagged;
            }

            String tag = getString(profile, "tag", "");
            if (!tag.isEmpty()) {
                return tag + " " + plainName;
            }

            return plainName;
        }

        String prefix = normalizeFormatting(getString(playerObject, "prefix", ""));
        if (!prefix.isEmpty()) {
            return prefix + " " + plainName;
        }

        String staffRank = getString(playerObject, "rank", "");
        if (!staffRank.isEmpty() && !"NONE".equalsIgnoreCase(staffRank)) {
            return formatStaffRank(staffRank) + " " + plainName;
        }

        String newPackageRank = getString(playerObject, "newPackageRank", "");
        String packageRank = getString(playerObject, "packageRank", "");
        String monthlyRank = getString(playerObject, "monthlyPackageRank", "");

        String plusColor = colorForHypixelRank(
            getString(playerObject, "rankPlusColor", "RED")
        );
        String monthlyColor = colorForHypixelRank(
            getString(playerObject, "monthlyRankColor", "GOLD")
        );

        if ("SUPERSTAR".equalsIgnoreCase(monthlyRank)) {
            return monthlyColor + "[MVP" + plusColor + "++" + monthlyColor + "] " + plainName;
        }
        if (
            "MVP_PLUS".equalsIgnoreCase(newPackageRank) ||
            "MVP_PLUS".equalsIgnoreCase(packageRank)
        ) {
            return "§b[MVP" + plusColor + "+§b] " + plainName;
        }
        if (
            "MVP".equalsIgnoreCase(newPackageRank) ||
            "MVP".equalsIgnoreCase(packageRank)
        ) {
            return "§b[MVP] " + plainName;
        }
        if (
            "VIP_PLUS".equalsIgnoreCase(newPackageRank) ||
            "VIP_PLUS".equalsIgnoreCase(packageRank)
        ) {
            return "§a[VIP§6+§a] " + plainName;
        }
        if (
            "VIP".equalsIgnoreCase(newPackageRank) ||
            "VIP".equalsIgnoreCase(packageRank)
        ) {
            return "§a[VIP] " + plainName;
        }

        return plainName;
    }

    private static String normalizeFormatting(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('&', '§').trim();
    }

    private static String formatStaffRank(String rank) {
        if (rank == null || rank.isEmpty()) {
            return "";
        }

        switch (rank.toUpperCase()) {
            case "ADMIN":
                return "§c[ADMIN]";
            case "MODERATOR":
                return "§2[MOD]";
            case "HELPER":
                return "§9[HELPER]";
            case "YOUTUBER":
                return "§c[§fYOUTUBE§c]";
            case "GAME_MASTER":
                return "§2[GM]";
            case "OWNER":
                return "§c[OWNER]";
            case "NORMAL":
            case "NONE":
                return "";
            default:
                return "§f[" + rank + "]";
        }
    }

    private static String colorForHypixelRank(String color) {
        if (color == null || color.isEmpty()) {
            return "§f";
        }

        switch (color.toUpperCase()) {
            case "BLACK":
                return "§0";
            case "DARK_BLUE":
                return "§1";
            case "DARK_GREEN":
                return "§2";
            case "DARK_AQUA":
                return "§3";
            case "DARK_RED":
                return "§4";
            case "DARK_PURPLE":
                return "§5";
            case "GOLD":
                return "§6";
            case "GRAY":
                return "§7";
            case "DARK_GRAY":
                return "§8";
            case "BLUE":
                return "§9";
            case "GREEN":
                return "§a";
            case "AQUA":
                return "§b";
            case "RED":
                return "§c";
            case "LIGHT_PURPLE":
                return "§d";
            case "YELLOW":
                return "§e";
            case "WHITE":
            default:
                return "§f";
        }
    }

    private static JsonObject getPlayerObject(JsonObject root, ProviderId providerId) {
        if (providerId == ProviderId.ABYSS) {
            if (!getBoolean(root, "success", false)) {
                return null;
            }
            return getObject(root, "player");
        }

        if (providerId == ProviderId.HYPIXEL_PUBLIC) {
            if (!getBoolean(root, "success", false)) {
                return null;
            }
            return getObject(root, "player");
        }

        return root;
    }

    private static JsonObject getObject(JsonObject object, String key) {
        if (object == null || !object.has(key)) {
            return new JsonObject();
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull() || !element.isJsonObject()) {
            return new JsonObject();
        }

        return element.getAsJsonObject();
    }

    private static int getInt(JsonObject object, String key, int fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            if (element.getAsJsonPrimitive().isNumber()) {
                return element.getAsInt();
            }
            return (int) Double.parseDouble(element.getAsString());
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            return element.getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean getBoolean(JsonObject object, String key, boolean fallback) {
        if (object == null || !object.has(key)) {
            return fallback;
        }

        JsonElement element = object.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }

        try {
            return element.getAsBoolean();
        } catch (Exception e) {
            return fallback;
        }
    }
}
