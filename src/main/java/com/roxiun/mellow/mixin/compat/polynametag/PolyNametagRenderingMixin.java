package com.roxiun.mellow.mixin.compat.polynametag;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.nametag.NametagClientIconRenderer;
import com.roxiun.mellow.util.nametag.NametagRenderContext;
import net.minecraft.client.gui.FontRenderer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "org.polyfrost.polynametag.render.NametagRenderingKt", remap = false)
public class PolyNametagRenderingMixin {

    @Dynamic
    @ModifyArgs(
        method = "drawBackground",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/opengl/GL11;glColor4f(FFFF)V"
        ),
        remap = false,
        require = 0
    )
    private static void mellow$setBackgroundColor(Args args) {
        MellowOneConfig config = Mellow.config;
        if (
            config == null ||
            !config.coloredNametagBackgrounds ||
            !config.coloredNametagAffectPolyNametag ||
            !NametagRenderContext.isActive()
        ) {
            return;
        }

        OneColor color = NametagRenderContext.getColor();
        if (color == null) {
            return;
        }

        args.set(0, color.getRed() / 255f);
        args.set(1, color.getGreen() / 255f);
        args.set(2, color.getBlue() / 255f);
    }

    @Dynamic
    @Redirect(
        method = {
            "drawFrontBackground(Ljava/lang/String;Lnet/minecraft/entity/Entity;)V",
            "drawFrontBackground(Ljava/lang/String;IIIIILnet/minecraft/entity/Entity;)V",
        },
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;getStringWidth(Ljava/lang/String;)I"
        ),
        remap = false,
        require = 0
    )
    private static int mellow$expandBackgroundWidth(
        FontRenderer fontRenderer,
        String text
    ) {
        return NametagClientIconRenderer.adjustWidth(
            text,
            fontRenderer.getStringWidth(text)
        );
    }

    @Dynamic
    @Inject(
        method = "drawStringWithoutZFighting(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;FFI)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;translate(FFF)V",
            shift = At.Shift.AFTER
        ),
        remap = false,
        require = 0
    )
    private static void mellow$drawClientIcon(
        FontRenderer fontRenderer,
        String text,
        float x,
        float y,
        int color,
        CallbackInfoReturnable<Integer> cir
    ) {
        NametagClientIconRenderer.drawActiveIcon(
            fontRenderer,
            text,
            NametagClientIconRenderer.adjustTextX(text, x),
            y,
            color
        );
    }

    @Dynamic
    @ModifyArgs(
        method = "drawStringWithoutZFighting(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;FFI)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;FFIZ)I",
            ordinal = 0
        ),
        remap = false,
        require = 0
    )
    private static void mellow$shiftNormalTextX(Args args) {
        String text = (String) args.get(0);
        float x = (Float) args.get(1);
        args.set(1, NametagClientIconRenderer.adjustTextX(text, x));
    }

    @Dynamic
    @ModifyArgs(
        method = "drawStringWithoutZFighting(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;FFI)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/FontRenderer;drawString(Ljava/lang/String;FFIZ)I",
            ordinal = 1
        ),
        remap = false,
        require = 0
    )
    private static void mellow$shiftShadowTextX(Args args) {
        String text = (String) args.get(0);
        float x = (Float) args.get(1);
        args.set(1, NametagClientIconRenderer.adjustTextX(text, x));
    }

    @Dynamic
    @ModifyArgs(
        method = "drawStringWithoutZFighting(Lnet/minecraft/client/gui/FontRenderer;Ljava/lang/String;FFI)I",
        at = @At(
            value = "INVOKE",
            target = "Lcc/polyfrost/oneconfig/renderer/TextRenderer;drawBorderedText(Ljava/lang/String;FFII)I"
        ),
        remap = false,
        require = 0
    )
    private static void mellow$shiftBorderedTextX(Args args) {
        String text = (String) args.get(0);
        float x = (Float) args.get(1);
        args.set(1, NametagClientIconRenderer.adjustTextX(text, x));
    }
}
