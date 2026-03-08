package com.roxiun.mellow.feature.stats;

import com.roxiun.mellow.api.provider.model.FetchFailureReason;
import com.roxiun.mellow.cache.ProfileFetchResult;
import org.junit.Assert;
import org.junit.Test;

public class StatsFetchFailureFormatterTest {

    @Test
    public void describesMissingInGameUuidClearly() {
        ProfileFetchResult result = ProfileFetchResult.failure(
            FetchFailureReason.UUID_UNAVAILABLE,
            "No in-game UUID available",
            "Abyss"
        );

        Assert.assertEquals(
            "no in-game UUID available yet",
            StatsFetchFailureFormatter.describe(result)
        );
    }

    @Test
    public void describesMissingApiKeyWithProviderName() {
        ProfileFetchResult result = ProfileFetchResult.failure(
            FetchFailureReason.MISSING_API_KEY,
            "Missing API key",
            "Hypixel Public API"
        );

        Assert.assertEquals(
            "Hypixel Public API needs an API key",
            StatsFetchFailureFormatter.describe(result)
        );
    }

    @Test
    public void describesNoPlayerDataAsPossibleNick() {
        ProfileFetchResult result = ProfileFetchResult.failure(
            FetchFailureReason.NO_PLAYER_DATA,
            "Provider returned no player data",
            "Abyss"
        );

        Assert.assertEquals(
            "Abyss returned no player data (possibly nicked)",
            StatsFetchFailureFormatter.describe(result)
        );
    }

    @Test
    public void describesHiddenProfilesWithoutProviderNoise() {
        ProfileFetchResult result = ProfileFetchResult.failure(
            FetchFailureReason.NO_PLAYER_DATA,
            "Player is nicked or an NPC",
            "Abyss"
        );

        Assert.assertEquals(
            "player is nicked or an NPC",
            StatsFetchFailureFormatter.describe(result)
        );
    }
}
