package com.roxiun.mellow.module.bedwars;

import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.module.GameModule;

public class BedwarsModule implements GameModule {

    private final BedwarsTimerService timerService = new BedwarsTimerService();

    @Override
    public String getId() {
        return "bedwars";
    }

    @Override
    public void onTick(GameSnapshot snapshot) {
        timerService.update(snapshot);
    }

    public void reset() {
        timerService.reset();
    }

    public BedwarsTimerState getTimerState() {
        return timerService.getState();
    }
}
