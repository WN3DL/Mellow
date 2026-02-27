package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.provider.model.StatScope;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.feature.stats.tab.ExtendedStatsTabOverlay;
import com.roxiun.mellow.feature.stats.tab.ExtendedTabStatsMode;
import com.roxiun.mellow.gamestate.GameSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TabOverlayRouter {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private ExtendedStatsTabOverlay overlay;

    private boolean wasActiveLastFrame;
    private StatScope lastScope;
    private long lastStateVersion = Long.MIN_VALUE;
    private int lastDimensionId = Integer.MIN_VALUE;

    public TabOverlayRouter(MellowOneConfig config) {
        this.config = config;
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onRenderPlayerList(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.PLAYER_LIST) {
            return;
        }

        StatScope scope = ExtendedTabStatsMode.resolveScope();
        if (!isExtendedModeActive(scope)) {
            resetIfNeeded();
            return;
        }

        if (!isTabKeyDown()) {
            resetIfNeeded();
            // Suppress any third-party trailing tab animation frames while extended mode is active.
            event.setCanceled(true);
            return;
        }

        ExtendedStatsTabOverlay statsOverlay = getOverlay();
        if (statsOverlay == null) {
            return;
        }

        if (shouldResetScroll(scope)) {
            statsOverlay.resetScroll();
        }

        event.setCanceled(true);
        statsOverlay.renderExtendedPlayerList(scope);
    }

    public boolean isExtendedModeActive() {
        return isExtendedModeActive(ExtendedTabStatsMode.resolveScope());
    }

    public ExtendedStatsTabOverlay getOverlay() {
        if (overlay == null && mc != null && mc.ingameGUI != null && config != null) {
            overlay = new ExtendedStatsTabOverlay(mc, mc.ingameGUI, config);
        }
        return overlay;
    }

    private boolean isExtendedModeActive(StatScope scope) {
        return (
            config != null &&
            config.tabStats &&
            config.extendedTabStatsView &&
            scope != null &&
            mc != null &&
            mc.thePlayer != null &&
            mc.theWorld != null
        );
    }

    private boolean shouldResetScroll(StatScope scope) {
        long stateVersion = getStateVersion();
        int dimensionId = getDimensionId();

        boolean shouldReset =
            !wasActiveLastFrame ||
            scope != lastScope ||
            stateVersion != lastStateVersion ||
            dimensionId != lastDimensionId;

        wasActiveLastFrame = true;
        lastScope = scope;
        lastStateVersion = stateVersion;
        lastDimensionId = dimensionId;
        return shouldReset;
    }

    private void resetIfNeeded() {
        if (!wasActiveLastFrame) {
            return;
        }

        ExtendedStatsTabOverlay statsOverlay = getOverlay();
        if (statsOverlay != null) {
            statsOverlay.resetScroll();
        }

        wasActiveLastFrame = false;
        lastScope = null;
        lastStateVersion = Long.MIN_VALUE;
        lastDimensionId = Integer.MIN_VALUE;
    }

    private long getStateVersion() {
        GameSnapshot snapshot = HypixelFeatures.getInstance().getGameSnapshot();
        return snapshot == null ? Long.MIN_VALUE : snapshot.getStateVersion();
    }

    private int getDimensionId() {
        if (mc == null || mc.theWorld == null || mc.theWorld.provider == null) {
            return Integer.MIN_VALUE;
        }
        return mc.theWorld.provider.getDimensionId();
    }

    private boolean isTabKeyDown() {
        return (
            mc != null &&
            mc.gameSettings != null &&
            mc.gameSettings.keyBindPlayerList != null &&
            mc.gameSettings.keyBindPlayerList.isKeyDown()
        );
    }
}
