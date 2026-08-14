package com.roxiun.mellow.util.ping;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionPingFetchGate {

    private final Set<String> requestedPlayers = ConcurrentHashMap.newKeySet();

    public boolean tryMarkRequested(String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            return false;
        }

        return requestedPlayers.add(playerId);
    }

    public void clearPlayer(String playerId) {
        if (playerId == null || playerId.trim().isEmpty()) {
            return;
        }

        requestedPlayers.remove(playerId);
    }

    public void clear() {
        requestedPlayers.clear();
    }
}
