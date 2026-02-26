package com.roxiun.mellow;

import com.roxiun.mellow.anticheat.AnticheatManager;
import com.roxiun.mellow.api.aurora.AuroraApi;
import com.roxiun.mellow.api.duels.PlanckeApi;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.provider.AbyssApi;
import com.roxiun.mellow.api.provider.HypixelPublicApi;
import com.roxiun.mellow.api.provider.NadeshikoApi;
import com.roxiun.mellow.api.provider.ProviderManager;
import com.roxiun.mellow.api.provider.StatsProvider;
import com.roxiun.mellow.api.seraph.SeraphApi;
import com.roxiun.mellow.api.urchin.UrchinApi;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.commands.*;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.TabStats;
import com.roxiun.mellow.events.ChatHandler;
import com.roxiun.mellow.events.EmeraldTimerHandler;
import com.roxiun.mellow.events.WorldLoadHandler;
import com.roxiun.mellow.task.StatsChecker;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.nicks.NickUtils;
import com.roxiun.mellow.util.nicks.NumberDenicker;
import com.roxiun.mellow.util.player.PregameStats;
import com.roxiun.mellow.util.tags.TagUtils;
import java.util.HashMap;
import java.util.Map;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@Mod(modid = Mellow.MODID, name = Mellow.NAME, version = Mellow.VERSION)
public class Mellow {

    public static final String MODID = "mellow";
    public static final String NAME = "Mellow";
    public static final String VERSION = "6.0.0";

    public static MellowOneConfig config;
    public static final Map<String, TabStats> tabStats = new HashMap<>();
    public static NickUtils nickUtils;

    public static MojangApi mojangApi;
    public static UrchinApi urchinApi;
    public static SeraphApi seraphApi;
    public static PlayerCache playerCache;
    public static BlacklistManager blacklistManager;
    private static AnticheatManager anticheatManager;

    private ProviderManager providerManager;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        config = new MellowOneConfig();

        HypixelFeatures.getInstance().initialize();

        anticheatManager = new AnticheatManager(this);
        blacklistManager = new BlacklistManager();

        mojangApi = new MojangApi();
        providerManager = new ProviderManager();
        providerManager.register(new HypixelPublicApi(mojangApi, config));
        providerManager.register(new NadeshikoApi(mojangApi));
        providerManager.register(new AbyssApi(mojangApi));

        urchinApi = new UrchinApi(mojangApi);
        seraphApi = new SeraphApi(mojangApi);
        PlanckeApi planckeApi = new PlanckeApi();
        AuroraApi auroraApi = new AuroraApi();

        playerCache = new PlayerCache(
            mojangApi,
            providerManager,
            urchinApi,
            seraphApi,
            config.urchinKey,
            config.seraphKey,
            config
        );

        nickUtils = new NickUtils(playerCache, config);

        TagUtils tagUtils = new TagUtils(this, blacklistManager);
        NumberDenicker numberDenicker = new NumberDenicker(
            config,
            nickUtils,
            auroraApi
        );
        PregameStats pregameStats = new PregameStats(
            playerCache,
            config,
            blacklistManager
        );

        StatsChecker statsChecker = new StatsChecker(
            playerCache,
            nickUtils,
            config,
            tabStats,
            tagUtils,
            blacklistManager
        );

        MinecraftForge.EVENT_BUS.register(
            new ChatHandler(
                config,
                nickUtils,
                numberDenicker,
                pregameStats,
                planckeApi,
                statsChecker,
                playerCache
            )
        );
        MinecraftForge.EVENT_BUS.register(
            new WorldLoadHandler(numberDenicker, pregameStats, nickUtils)
        );
        MinecraftForge.EVENT_BUS.register(
            new EmeraldTimerHandler(HypixelFeatures.getInstance())
        );

        ClientCommandHandler.instance.registerCommand(
            new BedwarsCommand(playerCache, config)
        );
        ClientCommandHandler.instance.registerCommand(new MellowCommand());
        ClientCommandHandler.instance.registerCommand(new DebugStateCommand());
        ClientCommandHandler.instance.registerCommand(
            new ClearCacheCommand(playerCache, tabStats)
        );
        ClientCommandHandler.instance.registerCommand(
            new DenickCommand(config, auroraApi)
        );
        ClientCommandHandler.instance.registerCommand(
            new SkinDenickCommand(playerCache)
        );
        ClientCommandHandler.instance.registerCommand(
            new BlacklistCommand(blacklistManager, mojangApi)
        );
        ClientCommandHandler.instance.registerCommand(
            new UrchinCommand(urchinApi, mojangApi, config)
        );
        ClientCommandHandler.instance.registerCommand(
            new SeraphCommand(seraphApi, mojangApi, config)
        );
    }

    public StatsProvider getStatsProvider() {
        if (providerManager == null) {
            return null;
        }
        return providerManager.getSelectedProvider(config);
    }

    public static AnticheatManager getAnticheatManager() {
        return anticheatManager;
    }
}
