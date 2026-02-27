package com.roxiun.mellow.mixin;

import com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsMode;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "org.polyfrost.vanillahud.hud.TabList$TabHud")
public class VanillaHudTabBackgroundCompatMixin {

    @Dynamic
    @Inject(method = "drawBG", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mellow$cancelVanillaHudBackground(CallbackInfo ci) {
        if (ExtendedTabStatsMode.isExtendedModeActiveNow()) {
            ci.cancel();
        }
    }
}
