package com.roxiun.mellow.gamestate;

import net.hypixel.data.type.GameType;

public class GameSnapshot {

    private static final GameSnapshot EMPTY = new GameSnapshot(
        false,
        "",
        null,
        "",
        "",
        false,
        false,
        PartyState.empty(),
        0L
    );

    private final boolean onHypixel;
    private final String serverName;
    private final GameType gameType;
    private final String mode;
    private final String map;
    private final boolean lobby;
    private final boolean pregame;
    private final PartyState partyState;
    private final long updatedAt;

    public GameSnapshot(
        boolean onHypixel,
        String serverName,
        GameType gameType,
        String mode,
        String map,
        boolean lobby,
        boolean pregame,
        PartyState partyState,
        long updatedAt
    ) {
        this.onHypixel = onHypixel;
        this.serverName = serverName;
        this.gameType = gameType;
        this.mode = mode;
        this.map = map;
        this.lobby = lobby;
        this.pregame = pregame;
        this.partyState = partyState;
        this.updatedAt = updatedAt;
    }

    public static GameSnapshot empty() {
        return EMPTY;
    }

    public boolean isOnHypixel() {
        return onHypixel;
    }

    public String getServerName() {
        return serverName;
    }

    public GameType getGameType() {
        return gameType;
    }

    public String getMode() {
        return mode;
    }

    public String getMap() {
        return map;
    }

    public boolean isLobby() {
        return lobby;
    }

    public boolean isPregame() {
        return pregame;
    }

    public PartyState getPartyState() {
        return partyState;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public boolean isInBedwars() {
        return gameType == GameType.BEDWARS && !lobby;
    }

    public boolean isInBedwarsMatch() {
        return isInBedwars() && !pregame;
    }

    public GameSnapshot withPregame(boolean inPregame) {
        return new GameSnapshot(
            onHypixel,
            serverName,
            gameType,
            mode,
            map,
            lobby,
            inPregame,
            partyState,
            System.currentTimeMillis()
        );
    }

    public GameSnapshot withPartyState(PartyState newPartyState) {
        return new GameSnapshot(
            onHypixel,
            serverName,
            gameType,
            mode,
            map,
            lobby,
            pregame,
            newPartyState,
            System.currentTimeMillis()
        );
    }

    public GameSnapshot withConnection(boolean hypixel) {
        return new GameSnapshot(
            hypixel,
            serverName,
            gameType,
            mode,
            map,
            lobby,
            pregame,
            partyState,
            System.currentTimeMillis()
        );
    }
}
