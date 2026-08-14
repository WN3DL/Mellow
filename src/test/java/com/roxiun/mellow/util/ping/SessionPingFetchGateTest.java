package com.roxiun.mellow.util.ping;

import org.junit.Assert;
import org.junit.Test;

public class SessionPingFetchGateTest {

    @Test
    public void onlyAllowsFirstRequestPerPlayer() {
        SessionPingFetchGate gate = new SessionPingFetchGate();

        Assert.assertTrue(gate.tryMarkRequested("player-uuid"));
        Assert.assertFalse(gate.tryMarkRequested("player-uuid"));
    }

    @Test
    public void clearPlayerAllowsAnotherRequest() {
        SessionPingFetchGate gate = new SessionPingFetchGate();

        Assert.assertTrue(gate.tryMarkRequested("player-uuid"));
        gate.clearPlayer("player-uuid");

        Assert.assertTrue(gate.tryMarkRequested("player-uuid"));
    }

    @Test
    public void clearAllowsAnotherRequest() {
        SessionPingFetchGate gate = new SessionPingFetchGate();

        Assert.assertTrue(gate.tryMarkRequested("player-uuid"));
        gate.clear();

        Assert.assertTrue(gate.tryMarkRequested("player-uuid"));
    }
}
