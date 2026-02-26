package com.roxiun.mellow.events;

import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class EmeraldTimerHandler {

    private final HypixelFeatures hypixelFeatures;

    public EmeraldTimerHandler(HypixelFeatures hypixelFeatures) {
        this.hypixelFeatures = hypixelFeatures;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            hypixelFeatures.onClientTick();
        }
    }
}
