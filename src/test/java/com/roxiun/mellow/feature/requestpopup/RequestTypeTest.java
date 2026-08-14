package com.roxiun.mellow.feature.requestpopup;

import org.junit.Assert;
import org.junit.Test;

public class RequestTypeTest {

    @Test
    public void friendRequestsKeepAcceptAndDenyCommands() {
        Assert.assertEquals(
            "/friend accept Example",
            RequestType.FRIEND.buildCommand(true, "Example")
        );
        Assert.assertEquals(
            "/friend deny Example",
            RequestType.FRIEND.buildCommand(false, "Example")
        );
    }

    @Test
    public void partyInvitesOnlySendAcceptCommand() {
        Assert.assertEquals(
            "/party accept Example",
            RequestType.PARTY.buildCommand(true, "Example")
        );
        Assert.assertNull(RequestType.PARTY.buildCommand(false, "Example"));
    }
}
