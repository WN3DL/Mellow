package com.roxiun.mellow.api.seraph;

import java.util.Locale;
import net.minecraft.util.ResourceLocation;

public enum SeraphClientType {
    ESSENTIAL("Essential", "mellow:textures/clients/essential.png", 256),
    LABYMOD("LabyMod", "mellow:textures/clients/laby.png", 256),
    BADLION("Badlion", "mellow:textures/clients/badlion.png", 256),
    LUNAR("Lunar", "mellow:textures/clients/lunar.png", 512),
    FEATHER("Feather", "mellow:textures/clients/feather.png", 256),
    UNKNOWN("Unknown", "mellow:textures/clients/unknown.png", 512);

    private final String displayName;
    private final ResourceLocation texture;
    private final float textureSize;

    SeraphClientType(String displayName, String texturePath, int textureSize) {
        this.displayName = displayName;
        this.texture = new ResourceLocation(texturePath);
        this.textureSize = textureSize;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ResourceLocation getTexture() {
        return texture;
    }

    public float getTextureSize() {
        return textureSize;
    }

    public static SeraphClientType fromDetectedName(String clientName) {
        if (clientName == null || clientName.trim().isEmpty()) {
            return null;
        }

        String normalized = clientName
            .trim()
            .toUpperCase(Locale.ROOT)
            .replace(" ", "");

        if (normalized.contains("ESSENTIAL")) {
            return ESSENTIAL;
        }
        if (normalized.contains("LABY")) {
            return LABYMOD;
        }
        if (normalized.contains("BADLION")) {
            return BADLION;
        }
        if (normalized.contains("LUNAR")) {
            return LUNAR;
        }
        if (normalized.contains("FEATHER")) {
            return FEATHER;
        }

        return UNKNOWN;
    }
}
