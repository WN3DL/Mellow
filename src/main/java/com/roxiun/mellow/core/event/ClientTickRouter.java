package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.feature.replay.ReplayManager;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class ClientTickRouter {

    private final HypixelFeatures hypixelFeatures;

    public ClientTickRouter(HypixelFeatures hypixelFeatures) {
        this.hypixelFeatures = hypixelFeatures;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            hypixelFeatures.onClientTick();
            ReplayManager
                .getInstance()
                .onClientTick(hypixelFeatures.getGameSnapshot());
        }
    }
}
