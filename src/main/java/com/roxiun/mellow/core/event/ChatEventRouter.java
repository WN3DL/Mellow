package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.duels.PlanckeApi;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.module.bedwars.BedwarsChatSignalParser;
import com.roxiun.mellow.task.StatsChecker;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.StringUtils;
import com.roxiun.mellow.util.nicks.NickUtils;
import com.roxiun.mellow.util.nicks.NumberDenicker;
import com.roxiun.mellow.util.player.PregameStats;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatEventRouter {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private final NickUtils nickUtils;
    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final PlanckeApi planckeApi;
    private final StatsChecker statsChecker;

    public ChatEventRouter(
        MellowOneConfig config,
        NickUtils nickUtils,
        NumberDenicker numberDenicker,
        PregameStats pregameStats,
        PlanckeApi planckeApi,
        StatsChecker statsChecker
    ) {
        this.config = config;
        this.nickUtils = nickUtils;
        this.numberDenicker = numberDenicker;
        this.pregameStats = pregameStats;
        this.planckeApi = planckeApi;
        this.statsChecker = statsChecker;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        numberDenicker.onChat(event);
        pregameStats.onChat(event);

        String message = event.message.getUnformattedText();
        HypixelFeatures.getInstance().onChat(message);

        if (
            BedwarsChatSignalParser.isBedwarsStartMessage(message) ||
            BedwarsChatSignalParser.isBedwarsRespawnMessage(message)
        ) {
            if (config.autoWho) {
                mc.thePlayer.sendChatMessage("/who");
            }
        }

        if (message.startsWith("ONLINE:")) {
            String playersString = message.substring("ONLINE:".length()).trim();
            String[] players = playersString.split(",\\s*");
            List<String> onlinePlayers = new ArrayList<>(
                Arrays.asList(players)
            );
            nickUtils.updateNickedPlayers(onlinePlayers);
            statsChecker.checkPlayerStats(onlinePlayers);
        }

        if (message.startsWith(" ") && message.contains("Opponent:")) {
            String username = StringUtils.parseUsername(message);
            AsyncExecutor.getInstance().chat(() -> {
                try {
                    String stats = planckeApi.checkDuels(username);
                    ChatUtils.sendMessage(stats);
                } catch (IOException e) {
                    ChatUtils.sendMessage(
                        "§cFailed to get stats for " + username
                    );
                }
            });
        }
    }
}
