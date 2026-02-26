package com.roxiun.mellow.api.hypixel;

import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.gamestate.GameStateManager;
import com.roxiun.mellow.gamestate.PartyState;
import com.roxiun.mellow.module.ModuleManager;
import com.roxiun.mellow.module.bedwars.BedwarsModule;
import net.hypixel.data.type.GameType;

public class HypixelFeatures {

    private static final HypixelFeatures INSTANCE = new HypixelFeatures();

    private final GameStateManager gameStateManager = new GameStateManager();
    private final ModuleManager moduleManager = new ModuleManager();
    private final BedwarsModule bedwarsModule = new BedwarsModule();

    private boolean initialized;

    public static HypixelFeatures getInstance() {
        return INSTANCE;
    }

    private HypixelFeatures() {
        moduleManager.registerModule(bedwarsModule);
    }

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        gameStateManager.initialize();
        initialized = true;
    }

    public void onClientTick() {
        initialize();
        gameStateManager.onClientTick();
        moduleManager.tick(gameStateManager.getSnapshot());
    }

    public void onChat(String message) {
        moduleManager.chat(message, gameStateManager.getSnapshot());
    }

    public void onWorldChange() {
        gameStateManager.onWorldChange();
        bedwarsModule.reset();
    }

    public GameSnapshot getGameSnapshot() {
        return gameStateManager.getSnapshot();
    }

    public PartyState getPartyState() {
        return gameStateManager.getSnapshot().getPartyState();
    }

    public boolean isInBedwars() {
        return gameStateManager.getSnapshot().isInBedwarsMatch();
    }

    public boolean isInBedwarsSession() {
        return gameStateManager.getSnapshot().isInBedwars();
    }

    public boolean isInPregameLobby() {
        GameSnapshot snapshot = gameStateManager.getSnapshot();
        return snapshot.getGameType() == GameType.BEDWARS && snapshot.isPregame();
    }

    public boolean isInGameType(GameType gameType) {
        GameSnapshot snapshot = gameStateManager.getSnapshot();
        return snapshot.getGameType() == gameType && !snapshot.isLobby();
    }

    public String getMode() {
        return gameStateManager.getSnapshot().getMode();
    }

    public String getMap() {
        return gameStateManager.getSnapshot().getMap();
    }

    public String getEmeraldCounterText() {
        return bedwarsModule.getTimerState().getEmeraldDisplayText();
    }

    public String getDiamondCounterText() {
        return bedwarsModule.getTimerState().getDiamondDisplayText();
    }

    public int getEmeraldCounterTime() {
        return bedwarsModule.getTimerState().getEmeraldNext();
    }

    public int getEmeraldSpawnCount() {
        return bedwarsModule.getTimerState().getEmeraldCount();
    }

    public int getDiamondCounterTime() {
        return bedwarsModule.getTimerState().getDiamondNext();
    }

    public int getDiamondSpawnCount() {
        return bedwarsModule.getTimerState().getDiamondCount();
    }

    // Compatibility methods retained for legacy call-sites.
    public void updateEmeraldTimer() {
        onClientTick();
    }

    public void setMode(String mode) {}

    public void resetEmeraldTimer() {
        bedwarsModule.reset();
    }

    public void startNewGame() {
        bedwarsModule.reset();
    }
}
