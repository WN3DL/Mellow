package com.roxiun.mellow.api.provider;

import com.roxiun.mellow.api.provider.model.ProviderId;
import com.roxiun.mellow.config.MellowOneConfig;
import java.util.EnumMap;
import java.util.Map;

public class ProviderManager {

    private final Map<ProviderId, StatsProvider> providers = new EnumMap<>(ProviderId.class);

    public void register(StatsProvider provider) {
        providers.put(provider.getProviderId(), provider);
    }

    public StatsProvider getProvider(ProviderId providerId) {
        return providers.get(providerId);
    }

    public StatsProvider getSelectedProvider(MellowOneConfig config) {
        ProviderId selected = ProviderId.HYPIXEL_PUBLIC;

        if (config != null) {
            if (config.statsProvider == 1) {
                selected = ProviderId.NADESHIKO;
            } else if (config.statsProvider == 2) {
                selected = ProviderId.ABYSS;
            }
        }

        StatsProvider provider = providers.get(selected);
        if (provider != null) {
            return provider;
        }

        if (providers.containsKey(ProviderId.HYPIXEL_PUBLIC)) {
            return providers.get(ProviderId.HYPIXEL_PUBLIC);
        }
        if (providers.containsKey(ProviderId.NADESHIKO)) {
            return providers.get(ProviderId.NADESHIKO);
        }
        return providers.get(ProviderId.ABYSS);
    }
}
