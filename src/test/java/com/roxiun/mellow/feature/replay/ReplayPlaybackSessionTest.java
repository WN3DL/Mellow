package com.roxiun.mellow.feature.replay;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ReplayPlaybackSessionTest {

    @Test
    public void resolveControlActionMapsReplayHotbarSlots() {
        Assert.assertEquals(
            ReplayPlaybackSession.ControlAction.TELEPORT,
            ReplayPlaybackSession.resolveControlAction(0)
        );
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

    @Test
    public void sortTeleportTargetsOrdersByTeamThenPlayerName() {
        List<ReplayPlaybackSession.TeleportTarget> sorted =
            ReplayPlaybackSession.sortTeleportTargets(
                Arrays.asList(
                    new ReplayPlaybackSession.TeleportTarget(
                        "zoe",
                        "§czoe",
                        "red",
                        "§cRed"
                    ),
                    new ReplayPlaybackSession.TeleportTarget(
                        "bob",
                        "§9bob",
                        "blue",
                        "§9Blue"
                    ),
                    new ReplayPlaybackSession.TeleportTarget(
                        "amy",
                        "§9amy",
                        "blue",
                        "§9Blue"
                    ),
                    new ReplayPlaybackSession.TeleportTarget(
                        "solo",
                        "solo",
                        "\uFFFF",
                        "§7Unassigned"
                    )
                )
            );

        Assert.assertEquals("amy", sorted.get(0).getName());
        Assert.assertEquals("bob", sorted.get(1).getName());
        Assert.assertEquals("zoe", sorted.get(2).getName());
        Assert.assertEquals("solo", sorted.get(3).getName());
    }

    @Test
    public void normalizeHypixelTeamNameCollapsesSplitHypixelBuckets() {
        Assert.assertEquals(
            "Blue",
            ReplayPlaybackSession.normalizeHypixelTeamName("Blue0", "§9")
        );
        Assert.assertEquals(
            "Green",
            ReplayPlaybackSession.normalizeHypixelTeamName("Green10", "§a")
        );
        Assert.assertEquals(
            "Pink",
            ReplayPlaybackSession.normalizeHypixelTeamName("", "§d")
        );
    }
}
