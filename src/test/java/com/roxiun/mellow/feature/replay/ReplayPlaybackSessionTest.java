package com.roxiun.mellow.feature.replay;

import org.junit.Assert;
import org.junit.Test;

public class ReplayPlaybackSessionTest {

    @Test
    public void resolveControlActionMapsReplayHotbarSlots() {
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.SLOW_DOWN,
            ReplayPlaybackSession.resolveControlAction(2)
        );
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.BACKWARD,
            ReplayPlaybackSession.resolveControlAction(3)
        );
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.TOGGLE_PAUSE,
            ReplayPlaybackSession.resolveControlAction(4)
        );
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.FORWARD,
            ReplayPlaybackSession.resolveControlAction(5)
        );
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.SPEED_UP,
            ReplayPlaybackSession.resolveControlAction(6)
        );
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.STOP,
            ReplayPlaybackSession.resolveControlAction(8)
        );
    }

    @Test
    public void resolveControlActionIgnoresNonActionSlots() {
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.NONE,
            ReplayPlaybackSession.resolveControlAction(0)
        );
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.NONE,
            ReplayPlaybackSession.resolveControlAction(1)
        );
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.NONE,
            ReplayPlaybackSession.resolveControlAction(7)
        );
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.NONE,
            ReplayPlaybackSession.resolveControlAction(9)
        );
    }
}
