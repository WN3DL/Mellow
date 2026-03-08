package com.roxiun.mellow.feature.replay;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import net.minecraft.nbt.NBTTagCompound;
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

    @Test
    public void isAtEndTreatsDurationAndBeyondAsEnded() {
        Assert.assertFalse(ReplayPlaybackSession.isAtEnd(184999, 185000));
        Assert.assertTrue(ReplayPlaybackSession.isAtEnd(185000, 185000));
        Assert.assertTrue(ReplayPlaybackSession.isAtEnd(185001, 185000));
    }

    @Test
    public void buildSkullTextureValueEncodesTheTextureUrl() {
        String value = ReplayPlaybackSession.buildSkullTextureValue(
            "http://textures.minecraft.net/texture/example"
        );
        String decoded = new String(
            Base64.getDecoder().decode(value),
            StandardCharsets.UTF_8
        );

        Assert.assertEquals(
            "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/example\"}}}",
            decoded
        );
    }

    @Test
    public void withSkullOwnerTagPreservesExistingDisplayData() {
        NBTTagCompound existingTag = new NBTTagCompound();
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "Replay Control");
        existingTag.setTag("display", display);

        NBTTagCompound mergedTag = ReplayPlaybackSession.withSkullOwnerTag(
            existingTag,
            "http://textures.minecraft.net/texture/example"
        );

        Assert.assertEquals(
            "Replay Control",
            mergedTag.getCompoundTag("display").getString("Name")
        );
        Assert.assertTrue(mergedTag.hasKey("SkullOwner"));
    }
}
