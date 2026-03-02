package com.roxiun.mellow.feature.alerts;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;

public class AlertSoundGate {

    private final AtomicBoolean played = new AtomicBoolean(false);

    public boolean tryPlayPling(Minecraft mc, float volume, float pitch) {
        if (mc == null || mc.thePlayer == null) {
            return false;
        }
        if (!played.compareAndSet(false, true)) {
            return false;
        }

        mc.thePlayer.playSound("note.pling", volume, pitch);
        return true;
    }

    public void reset() {
        played.set(false);
    }
}
