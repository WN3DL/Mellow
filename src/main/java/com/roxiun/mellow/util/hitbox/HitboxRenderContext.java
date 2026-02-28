package com.roxiun.mellow.util.hitbox;

import net.minecraft.entity.Entity;

public final class HitboxRenderContext {

    private static final ThreadLocal<Entity> CURRENT_ENTITY = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> REENTRY_GUARD =
        ThreadLocal.withInitial(() -> false);

    private HitboxRenderContext() {}

    public static void setCurrentEntity(Entity entity) {
        CURRENT_ENTITY.set(entity);
    }

    public static Entity getCurrentEntity() {
        return CURRENT_ENTITY.get();
    }

    public static void clearCurrentEntity() {
        CURRENT_ENTITY.remove();
    }

    public static boolean tryEnterReentryGuard() {
        if (Boolean.TRUE.equals(REENTRY_GUARD.get())) {
            return false;
        }
        REENTRY_GUARD.set(true);
        return true;
    }

    public static void exitReentryGuard() {
        REENTRY_GUARD.set(false);
    }
}
