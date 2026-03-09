package com.roxiun.mellow.util.render;

import com.roxiun.mellow.api.seraph.SeraphClientType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

public final class SeraphClientIconRenderer {

    private SeraphClientIconRenderer() {}

    public static void drawIcon(
        SeraphClientType clientType,
        int x,
        int y,
        int size
    ) {
        drawIcon(clientType, x, y, size, 1.0F);
    }

    public static void drawIcon(
        SeraphClientType clientType,
        int x,
        int y,
        int size,
        float alpha
    ) {
        if (clientType == null || size <= 0) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.getTextureManager() == null) {
            return;
        }

        int textureSize = Math.round(clientType.getTextureSize());
        mc.getTextureManager().bindTexture(clientType.getTexture());
        GlStateManager.color(1.0F, 1.0F, 1.0F, clamp(alpha));
        GlStateManager.enableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        Gui.drawScaledCustomSizeModalRect(
            x,
            y,
            0.0F,
            0.0F,
            textureSize,
            textureSize,
            size,
            size,
            textureSize,
            textureSize
        );
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float clamp(float alpha) {
        if (alpha < 0.0F) {
            return 0.0F;
        }
        if (alpha > 1.0F) {
            return 1.0F;
        }
        return alpha;
    }
}
