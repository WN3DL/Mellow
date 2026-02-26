package com.roxiun.mellow.gamestate.query;

import com.roxiun.mellow.gamestate.GameSnapshot;
import net.hypixel.data.type.GameType;

public interface GameContext {
    GameSnapshot getSnapshot();

    default boolean isOnHypixel() {
        return getSnapshot().isOnHypixel();
    }

    default boolean isInGameType(GameType gameType) {
        GameSnapshot snapshot = getSnapshot();
        return snapshot.getGameType() == gameType && !snapshot.isLobby();
    }

    default boolean isBedwarsSession() {
        return getSnapshot().isInBedwars();
    }

    default boolean isBedwarsMatch() {
        return getSnapshot().isInBedwarsMatch();
    }

    default boolean isPregameBedwarsLobby() {
        GameSnapshot snapshot = getSnapshot();
        return snapshot.isInBedwars() && snapshot.isPregame();
    }
}
