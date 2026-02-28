package com.roxiun.mellow.util.nametag;

import cc.polyfrost.oneconfig.config.core.OneColor;

public final class NametagRenderContext {

    private static final ThreadLocal<OneColor> CURRENT_NAMETAG_COLOR =
        new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ACTIVE =
        ThreadLocal.withInitial(() -> false);

    private NametagRenderContext() {}

    public static void setActiveColor(OneColor color) {
        if (color == null) {
            clear();
            return;
        }
        CURRENT_NAMETAG_COLOR.set(color);
        ACTIVE.set(true);
    }

    public static OneColor getColor() {
        return CURRENT_NAMETAG_COLOR.get();
    }

    public static boolean isActive() {
        return Boolean.TRUE.equals(ACTIVE.get());
    }

    public static void clear() {
        CURRENT_NAMETAG_COLOR.remove();
        ACTIVE.remove();
    }
}
