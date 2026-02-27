package com.roxiun.mellow.core.event;

import com.roxiun.mellow.api.duels.PlanckeApi;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.feature.nicks.NumberDenicker;
import com.roxiun.mellow.feature.stats.PregameStats;
import com.roxiun.mellow.module.bedwars.BedwarsChatSignalParser;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.StringUtils;
import java.io.IOException;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ChatEventRouter {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final PlanckeApi planckeApi;

    public ChatEventRouter(
        MellowOneConfig config,
        NumberDenicker numberDenicker,
        PregameStats pregameStats,
        PlanckeApi planckeApi
    ) {
        this.config = config;
        this.numberDenicker = numberDenicker;
        this.pregameStats = pregameStats;
        this.planckeApi = planckeApi;
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
