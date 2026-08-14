package com.roxiun.mellow.module.bedwars;

import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.module.GameModule;

public class BedwarsModule implements GameModule {

    private final BedwarsTimerService timerService = new BedwarsTimerService();
    private final BedwarsUpgradesService upgradesService =
        new BedwarsUpgradesService();

    @Override
    public String getId() {
        return "bedwars";
    }

    @Override
    public void onTick(GameSnapshot snapshot) {
        timerService.update(snapshot);
        if (!snapshot.isInBedwars()) {
            upgradesService.reset();
        }
    }

    @Override
    public void onChat(String message, GameSnapshot snapshot) {
        boolean resetSignal =
            BedwarsChatSignalParser.isBedwarsStartMessage(message) ||
            BedwarsChatSignalParser.isBedwarsRespawnMessage(message) ||
            BedwarsChatSignalParser.isPregameCountdownMessage(message);

        if (resetSignal) {
            upgradesService.reset();
        }

        if (!snapshot.isInBedwars()) {
            return;
        }

        if (BedwarsChatSignalParser.isPurchaseMessage(message)) {
            upgradesService.processPurchaseMessage(message);
        }

        if (BedwarsChatSignalParser.isTrapSignalMessage(message)) {
            upgradesService.processTrapTriggeredMessage(message);
        }
    }

    public void reset() {
        timerService.reset();
        upgradesService.reset();
    }

    public BedwarsTimerState getTimerState() {
        return timerService.getState();
    }

    public BedwarsUpgradesService getUpgradesService() {
        return upgradesService;
    }
}
