package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.util.ChatUtils;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class ProviderHealthWarningService {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);
    private static final ProviderHealthWarningService INSTANCE =
        new ProviderHealthWarningService();

    private static volatile MellowOneConfig config;
    private static volatile boolean hasWarnedThisLaunch = false;

    private ProviderHealthWarningService() {}

    public static void init(MellowOneConfig configInstance) {
        config = configInstance;
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (hasWarnedThisLaunch) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || event.entity != mc.thePlayer) {
            return;
        }

        MellowOneConfig currentConfig = config;
        if (currentConfig == null) {
            hasWarnedThisLaunch = true;
            return;
        }

        boolean warned = false;

        if (currentConfig.statsProvider == 1) {
            ChatUtils.sendMessage(
                "§eNadeshiko is currently broken. We recommend switching your Stats Provider to §bAbyss§e in OneConfig."
            );
            warned = true;
        } else if (
            currentConfig.statsProvider == 0 &&
            !hasHypixelApiKey(currentConfig)
        ) {
            ChatUtils.sendMessage(
                "§eHypixel Public API is selected but no API key is set. We recommend switching your Stats Provider to §bAbyss§e in OneConfig."
            );
            warned = true;
        }

        if (warned) {
            hasWarnedThisLaunch = true;
        }
    }

    private static boolean hasHypixelApiKey(MellowOneConfig config) {
        return config.hypixelApiKey != null && !config.hypixelApiKey.trim().isEmpty();
    }
}
