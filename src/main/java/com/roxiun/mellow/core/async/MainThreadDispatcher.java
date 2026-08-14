package com.roxiun.mellow.core.async;

import net.minecraft.client.Minecraft;

public final class MainThreadDispatcher {

    private MainThreadDispatcher() {}

    public static void run(Runnable task) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        mc.addScheduledTask(task);
    }
}
