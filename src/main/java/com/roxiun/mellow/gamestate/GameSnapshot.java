package com.roxiun.mellow.gamestate;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
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
        PregameReason.NONE,
        "",
        Collections.emptyList(),
        PartyState.empty(),
        0L,
        0L
    );

    private final boolean onHypixel;
    private final String serverName;
    private final GameType gameType;
    private final String mode;
    private final String map;
    private final boolean lobby;
    private final boolean pregame;
    private final PregameReason pregameReason;
    private final String scoreboardTitle;
    private final List<String> scoreboardLines;
    private final PartyState partyState;
    private final long updatedAt;
    private final long stateVersion;

    public GameSnapshot(
        boolean onHypixel,
        String serverName,
        GameType gameType,
        String mode,
        String map,
        boolean lobby,
        boolean pregame,
        PregameReason pregameReason,
        String scoreboardTitle,
        List<String> scoreboardLines,
        PartyState partyState,
        long updatedAt,
        long stateVersion
    ) {
        this.onHypixel = onHypixel;
        this.serverName = serverName;
        this.gameType = gameType;
        this.mode = mode;
        this.map = map;
        this.lobby = lobby;
        this.pregame = pregame;
        this.pregameReason = pregameReason;
        this.scoreboardTitle = scoreboardTitle == null ? "" : scoreboardTitle;
        if (scoreboardLines == null) {
            this.scoreboardLines = Collections.emptyList();
        } else {
            this.scoreboardLines =
                Collections.unmodifiableList(new ArrayList<>(scoreboardLines));
        }
        this.partyState = partyState;
        this.updatedAt = updatedAt;
        this.stateVersion = stateVersion;
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

    public PregameReason getPregameReason() {
        return pregameReason;
    }

    public String getScoreboardTitle() {
        return scoreboardTitle;
    }

    public List<String> getScoreboardLines() {
        return scoreboardLines;
    }

    public PartyState getPartyState() {
        return partyState;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getStateVersion() {
        return stateVersion;
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
            inPregame ? PregameReason.PLAYERS_LINE : PregameReason.NONE,
            scoreboardTitle,
            scoreboardLines,
            partyState,
            System.currentTimeMillis(),
            stateVersion + 1
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
            pregameReason,
            scoreboardTitle,
            scoreboardLines,
            newPartyState,
            System.currentTimeMillis(),
            stateVersion + 1
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
            pregameReason,
            scoreboardTitle,
            scoreboardLines,
            partyState,
            System.currentTimeMillis(),
            stateVersion + 1
        );
    }

    public GameSnapshot withScoreboard(String title, List<String> lines) {
        return new GameSnapshot(
            onHypixel,
            serverName,
            gameType,
            mode,
            map,
            lobby,
            pregame,
            pregameReason,
            title,
            lines,
            partyState,
            System.currentTimeMillis(),
            stateVersion + 1
        );
    }

    public GameSnapshot withLocation(
        String newServerName,
        GameType newGameType,
        String newMode,
        String newMap,
        boolean inLobby,
        boolean inPregame,
        PregameReason reason,
        String title,
        List<String> lines
    ) {
        return new GameSnapshot(
            onHypixel,
            newServerName,
            newGameType,
            newMode,
            newMap,
            inLobby,
            inPregame,
            reason,
            title,
            lines,
            partyState,
            System.currentTimeMillis(),
            stateVersion + 1
        );
    }

    public boolean hasSameState(GameSnapshot other) {
        if (other == null) {
            return false;
        }

        return (
            onHypixel == other.onHypixel &&
            lobby == other.lobby &&
            pregame == other.pregame &&
            gameType == other.gameType &&
            pregameReason == other.pregameReason &&
            safe(serverName).equals(safe(other.serverName)) &&
            safe(mode).equals(safe(other.mode)) &&
            safe(map).equals(safe(other.map)) &&
            safe(scoreboardTitle).equals(safe(other.scoreboardTitle)) &&
            scoreboardLines.equals(other.scoreboardLines) &&
            partyState.equals(other.partyState)
        );
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
