package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.provider.model.PlayerIdentity;
import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.api.provider.model.ProviderResult;
import com.roxiun.mellow.api.provider.model.StatScope;
import java.io.IOException;

public interface StatsProvider {
    ProviderId getProviderId();

    String getDisplayName();

    default boolean requiresApiKey() {
        return false;
    }

    default boolean isConfigured() {
        return true;
    }

    BedwarsPlayer fetchPlayerStats(String playerName) throws IOException;

    String fetchPlayerData(String uuid);

    default ProviderResult<BedwarsPlayer> fetchStats(
        PlayerIdentity identity,
        StatScope scope
    ) {
        if (scope != StatScope.BEDWARS) {
            return ProviderResult.failure("Unsupported scope: " + scope);
        }

        try {
            BedwarsPlayer player = fetchPlayerStats(identity.getUsername());
            if (player == null) {
                return ProviderResult.failure("No data returned");
            }
            return ProviderResult.success(player);
        } catch (Exception e) {
            return ProviderResult.failure(e.getMessage());
        }
    }
}
