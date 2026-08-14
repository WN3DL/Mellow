package com.roxiun.mellow.data;

import org.junit.Assert;
import org.junit.Test;

public class PlayerProfileTabFormattingTest {

    @Test
    public void formatTabCountForDisplayAddsCommas() {
        Assert.assertEquals("§c12,345", PlayerProfile.formatTabCountForDisplay("§c12345"));
    }

    @Test
    public void formatTabCountForDisplayLeavesRatiosUnchanged() {
        Assert.assertEquals("§e1.23", PlayerProfile.formatTabCountForDisplay("§e1.23"));
    }
}
