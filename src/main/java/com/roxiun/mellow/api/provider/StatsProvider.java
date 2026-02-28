package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.duels.DuelsMode;
import com.roxiun.mellow.api.duels.DuelsPlayer;
import com.roxiun.mellow.api.skywars.SkywarsPlayer;
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

    default SkywarsPlayer fetchSkywarsStats(String playerName)
        throws IOException {
        return null;
    }

    default DuelsPlayer fetchDuelsStats(String playerName)
        throws IOException {
        return fetchDuelsStats(playerName, DuelsMode.OVERALL);
    }

    default DuelsPlayer fetchDuelsStats(String playerName, DuelsMode mode)
        throws IOException {
        return null;
    }

    String fetchPlayerData(String uuid);

    default ProviderResult<?> fetchStats(
        PlayerIdentity identity,
        StatScope scope
    ) {
        try {
            switch (scope) {
                case BEDWARS:
                    BedwarsPlayer bedwarsPlayer = fetchPlayerStats(
                        identity.getUsername()
                    );
                    if (bedwarsPlayer == null) {
                        return ProviderResult.failure("No data returned");
                    }
                    return ProviderResult.success(bedwarsPlayer);
                case SKYWARS:
                    SkywarsPlayer skywarsPlayer = fetchSkywarsStats(
                        identity.getUsername()
                    );
                    if (skywarsPlayer == null) {
                        return ProviderResult.failure("No data returned");
                    }
                    return ProviderResult.success(skywarsPlayer);
                case DUELS:
                    DuelsPlayer duelsPlayer = fetchDuelsStats(
                        identity.getUsername()
                    );
                    if (duelsPlayer == null) {
                        return ProviderResult.failure("No data returned");
                    }
                    return ProviderResult.success(duelsPlayer);
                default:
                    return ProviderResult.failure(
                        "Unsupported scope: " + scope
                    );
            }
        } catch (Exception e) {
            return ProviderResult.failure(e.getMessage());
        }
    }
}
