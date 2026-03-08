package com.roxiun.mellow.mixin;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.util.ping.PingProviderUtils;
import net.minecraft.client.network.NetworkPlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NetworkPlayerInfo.class)
public class PingMixin {

    @Shadow
    private int responseTime;

    @Inject(method = "getResponseTime", at = @At("HEAD"), cancellable = true)
    private void onGetResponseTime(CallbackInfoReturnable<Integer> cir) {
        int original = this.responseTime;

        String playerName =
            ((NetworkPlayerInfo) (Object) this).getGameProfile().getName();
        if (Mellow.nickUtils != null && Mellow.nickUtils.isNicked(playerName)) {
            cir.setReturnValue(original);
            return;
        }

        if (
            Mellow.config == null ||
            !HypixelFeatures.getInstance().getGameSnapshot().isOnHypixel()
        ) {
            cir.setReturnValue(original);
            return;
        }

        if (original > 1 && original < 999) {
            cir.setReturnValue(original);
            return;
        }

        String uuid = ((NetworkPlayerInfo) (Object) this).getGameProfile()
            .getId()
            .toString();

        boolean useLuna = PingProviderUtils.shouldUseLuna(Mellow.config);
        boolean useAurora = PingProviderUtils.shouldUseAurora(Mellow.config);

        if (!useAurora && !useLuna) {
            cir.setReturnValue(original);
            return;
        }

        if (useAurora) {
            if (
                Mellow.auroraPingService == null ||
                !PingProviderUtils.hasAuroraApiKey(Mellow.config)
            ) {
                cir.setReturnValue(original);
                return;
            }

            String compactUuid = uuid.replace("-", "");
            int cached = Mellow.auroraPingService.getCachedPing(compactUuid);
            if (cached >= 0) {
                cir.setReturnValue(cached);
                return;
            }

            cir.setReturnValue(original);
            Mellow.auroraPingService.fetchAsync(
                compactUuid,
                Mellow.config.auroraApiKey
            );
            return;
        }

        if (
            Mellow.lunaPingService == null ||
            !PingProviderUtils.hasLunaApiKey(Mellow.config)
        ) {
            cir.setReturnValue(original);
            return;
        }

        int cached = Mellow.lunaPingService.getCachedPing(uuid);
        if (cached >= 0) {
            cir.setReturnValue(cached);
            return;
        }

        cir.setReturnValue(original);
        Mellow.lunaPingService.fetchAsync(uuid, Mellow.config.lunaPingApiKey);
    }
}
