package com.roxiun.mellow.module;

import com.roxiun.mellow.gamestate.GameSnapshot;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ModuleManager {

    private final List<GameModule> modules = new CopyOnWriteArrayList<>();

    public void registerModule(GameModule module) {
        modules.add(module);
    }

    public void tick(GameSnapshot snapshot) {
        for (GameModule module : modules) {
            module.onTick(snapshot);
        }
    }

    public void chat(String message, GameSnapshot snapshot) {
        for (GameModule module : modules) {
            module.onChat(message, snapshot);
        }
    }
}
