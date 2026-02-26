package com.roxiun.mellow.util.formatting;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class BedwarsStarFormatter {

    private static final Map<Integer, PrestigeFormat> PRESTIGES = loadPrestiges();

    private BedwarsStarFormatter() {}

    public static String format(int level) {
        if (level < 0) {
            level = 0;
        }

        int prestigeIndex = level >= 5000 ? 50 : level / 100;
        PrestigeFormat format = PRESTIGES.get(prestigeIndex);

        while (format == null && prestigeIndex > 0) {
            prestigeIndex--;
            format = PRESTIGES.get(prestigeIndex);
        }

        if (format == null) {
            return "§f[" + level + "✫]";
        }

        return format.apply(level);
    }

    private static Map<Integer, PrestigeFormat> loadPrestiges() {
        Map<Integer, PrestigeFormat> map = new HashMap<>();

        try {
            InputStream stream = BedwarsStarFormatter.class
                .getClassLoader()
                .getResourceAsStream("assets/mellow/data/prestiges.json");

            if (stream == null) {
                return map;
            }

            JsonObject root = new JsonParser()
                .parse(new InputStreamReader(stream))
                .getAsJsonObject();
            JsonObject prestiges = root.getAsJsonObject("prestiges");

            for (Map.Entry<String, JsonElement> entry : prestiges.entrySet()) {
                int key = Integer.parseInt(entry.getKey());
                JsonObject value = entry.getValue().getAsJsonObject();

                String starSymbol = getString(value, "starSymbol", "✫");
                JsonObject colors = value.getAsJsonObject("colors");

                String leftBracket = getString(colors, "leftBracket", "§f");
                String rightBracket = getString(colors, "rightBracket", "");
                String starColor = getString(colors, "starColor", "");
                String[] numberColors = getStringArray(colors.getAsJsonArray("numberColors"));

                map.put(
                    key,
                    new PrestigeFormat(
                        leftBracket,
                        rightBracket,
                        starColor,
                        starSymbol,
                        numberColors
                    )
                );
            }
        } catch (Exception ignored) {}

        return map;
    }

    private static String getString(JsonObject object, String key, String fallback) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }

        try {
            return object.get(key).getAsString();
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String[] getStringArray(JsonArray array) {
        String[] values = new String[] { "", "", "", "" };
        if (array == null) {
            return values;
        }

        for (int i = 0; i < array.size() && i < values.length; i++) {
            try {
                values[i] = array.get(i).getAsString();
            } catch (Exception ignored) {
                values[i] = "";
            }
        }

        return values;
    }

    private static class PrestigeFormat {

        private final String leftBracket;
        private final String rightBracket;
        private final String starColor;
        private final String starSymbol;
        private final String[] numberColors;

        private PrestigeFormat(
            String leftBracket,
            String rightBracket,
            String starColor,
            String starSymbol,
            String[] numberColors
        ) {
            this.leftBracket = leftBracket;
            this.rightBracket = rightBracket;
            this.starColor = starColor;
            this.starSymbol = starSymbol;
            this.numberColors = numberColors;
        }

        private String apply(int level) {
            String value = String.valueOf(level);
            StringBuilder builder = new StringBuilder();

            builder.append(leftBracket);
            builder.append("[");

            for (int i = 0; i < value.length(); i++) {
                if (i < numberColors.length) {
                    builder.append(numberColors[i]);
                }
                builder.append(value.charAt(i));
            }

            builder.append(starColor);
            builder.append(starSymbol);
            builder.append(rightBracket);
            builder.append("]");

            return builder.toString();
        }
    }
}
