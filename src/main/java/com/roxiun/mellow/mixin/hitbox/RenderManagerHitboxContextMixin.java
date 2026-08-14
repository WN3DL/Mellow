package com.roxiun.mellow.mixin.hitbox;

import com.roxiun.mellow.util.hitbox.HitboxRenderContext;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderManager.class)
public class RenderManagerHitboxContextMixin {

    @Inject(method = "renderDebugBoundingBox", at = @At("HEAD"))
    private void mellow$captureHitboxEntity(
        Entity entityIn,
        double x,
        double y,
        double z,
        float entityYaw,
        float partialTicks,
        CallbackInfo ci
    ) {
        HitboxRenderContext.setCurrentEntity(entityIn);
    }

    @Inject(method = "renderDebugBoundingBox", at = @At("RETURN"))
    private void mellow$clearHitboxEntity(
        Entity entityIn,
        double x,
        double y,
        double z,
        float entityYaw,
        float partialTicks,
        CallbackInfo ci
    ) {
        HitboxRenderContext.clearCurrentEntity();
        HitboxRenderContext.exitReentryGuard();
    }
}
