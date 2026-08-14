package com.roxiun.mellow.mixin.nametag;

import com.roxiun.mellow.util.nametag.NametagClientIconRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.Render;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Render.class)
public class RenderNametagClientIconMixin {

    @Redirect(
        method = "renderLivingLabel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"
        ),
        require = 0
    )
    private int mellow$expandNametagWidth(FontRenderer fontRenderer, String text) {
        return NametagClientIconRenderer.adjustWidth(
            text,
            fontRenderer.getStringWidth(text)
        );
    }

    @Redirect(
        method = "renderLivingLabel",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;III)I"
        ),
        require = 0
    )
    private int mellow$drawNametagWithClientIcon(
        FontRenderer fontRenderer,
        String text,
        int x,
        int y,
        int color
    ) {
        int adjustedX = NametagClientIconRenderer.adjustTextX(text, x);
        NametagClientIconRenderer.drawActiveIcon(
            fontRenderer,
            text,
            adjustedX,
            y,
            color
        );
        return fontRenderer.drawString(text, adjustedX, y, color);
    }
}
