package com.roxiun.mellow.module;

import com.roxiun.mellow.gamestate.GameSnapshot;

public interface GameModule {
    String getId();

    default void onTick(GameSnapshot snapshot) {}

    default void onChat(String message, GameSnapshot snapshot) {}
}
