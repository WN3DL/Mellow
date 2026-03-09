package com.roxiun.mellow.util.nametag;

import cc.polyfrost.oneconfig.config.core.OneColor;
import com.roxiun.mellow.api.seraph.SeraphClientType;

public final class NametagRenderContext {

    private static final ThreadLocal<State> CURRENT_STATE = new ThreadLocal<>();

    private NametagRenderContext() {}

    public static void setState(
        OneColor color,
        SeraphClientType clientType,
        boolean clientIconLeft,
        String primaryLabelText
    ) {
        if (color == null && clientType == null) {
            clear();
            return;
        }
        CURRENT_STATE.set(
            new State(color, clientType, clientIconLeft, primaryLabelText)
        );
    }

    public static OneColor getColor() {
        State state = CURRENT_STATE.get();
        return state == null ? null : state.color;
    }

    public static SeraphClientType getClientType() {
        State state = CURRENT_STATE.get();
        return state == null ? null : state.clientType;
    }

    public static boolean isClientIconLeft() {
        State state = CURRENT_STATE.get();
        return state == null || state.clientIconLeft;
    }

    public static String getPrimaryLabelText() {
        State state = CURRENT_STATE.get();
        return state == null ? null : state.primaryLabelText;
    }

    public static boolean isActive() {
        return CURRENT_STATE.get() != null;
    }

    public static void clear() {
        CURRENT_STATE.remove();
    }

    private static final class State {

        private final OneColor color;
        private final SeraphClientType clientType;
        private final boolean clientIconLeft;
        private final String primaryLabelText;

        private State(
            OneColor color,
            SeraphClientType clientType,
            boolean clientIconLeft,
            String primaryLabelText
        ) {
            this.color = color;
            this.clientType = clientType;
            this.clientIconLeft = clientIconLeft;
            this.primaryLabelText = primaryLabelText;
        }
    }
}
