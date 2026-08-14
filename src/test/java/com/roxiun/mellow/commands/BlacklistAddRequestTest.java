package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.seraph.SeraphBlacklistReportType;
import org.junit.Assert;
import org.junit.Test;

public class BlacklistAddRequestTest {

    @Test
    public void parseDefaultsToLocalOnlyWhenReasonIsOmitted() {
        BlacklistAddRequest request = BlacklistAddRequest.parse(
            "/blacklist",
            new String[] { "add", "Alpha" }
        );

        Assert.assertEquals("Alpha", request.getPlayerName());
        Assert.assertEquals("(none)", request.getLocalReason());
        Assert.assertFalse(request.shouldSubmitToSeraph());
        Assert.assertNull(request.getSeraphReportType());
        Assert.assertNull(request.getSeraphReason());
    }

    @Test
    public void parseKeepsFreeformLocalReasonWhenSeraphIsNotExplicitlyRequested() {
        BlacklistAddRequest request = BlacklistAddRequest.parse(
            "/blacklist",
            new String[] { "add", "Alpha", "seraphic", "queue", "dodger" }
        );

        Assert.assertEquals("seraphic queue dodger", request.getLocalReason());
        Assert.assertFalse(request.shouldSubmitToSeraph());
    }

    @Test
    public void parseSeraphRequestUsesSharedReasonForLocalAndRemoteReport() {
        BlacklistAddRequest request = BlacklistAddRequest.parse(
            "/blacklist",
            new String[] { "add", "Alpha", "seraph", "ps", "queue", "sniper" }
        );

        Assert.assertEquals("queue sniper", request.getLocalReason());
        Assert.assertTrue(request.shouldSubmitToSeraph());
        Assert.assertEquals(
            SeraphBlacklistReportType.SNIPING_POTENTIAL,
            request.getSeraphReportType()
        );
        Assert.assertEquals("queue sniper", request.getSeraphReason());
    }

    @Test
    public void parseRejectsInvalidSeraphType() {
        try {
            BlacklistAddRequest.parse(
                "/blacklist",
                new String[] { "add", "Alpha", "seraph", "wat", "reason" }
            );
            Assert.fail("Expected invalid Seraph type to be rejected.");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(
                e.getMessage().contains("Valid types: cc|bc|s|ps|ls|a|bot|c|al")
            );
        }
    }
}
