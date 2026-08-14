package com.roxiun.mellow.api.util;

import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import org.junit.Assert;
import org.junit.Test;

public class HypixelApiUtilsParseResultTest {

    @Test
    public void parsePlayerDataResultFlagsMissingPlayerData() {
        ProviderResult<?> result = HypixelApiUtils.parsePlayerDataResult(
            "{}",
            ProviderId.HYPIXEL_PUBLIC
        );

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(
            FetchFailureReason.NO_PLAYER_DATA,
            result.getFailureReason()
        );
    }

    @Test
    public void parsePlayerDataResultFlagsInvalidJsonAsParseFailure() {
        ProviderResult<?> result = HypixelApiUtils.parsePlayerDataResult(
            "{not-json}",
            ProviderId.ABYSS
        );

        Assert.assertFalse(result.isSuccess());
        Assert.assertEquals(
            FetchFailureReason.PARSE_ERROR,
            result.getFailureReason()
        );
    }
}
