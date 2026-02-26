package com.roxiun.mellow.events;

import com.roxiun.mellow.api.duels.PlanckeApi;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.task.StatsChecker;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.StringUtils;
import com.roxiun.mellow.util.bedwars.BedwarsUpgradesTrapsManager;
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

public class ChatHandler {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final MellowOneConfig config;
    private final NickUtils nickUtils;
    private final NumberDenicker numberDenicker;
    private final PregameStats pregameStats;
    private final PlanckeApi planckeApi;
    private final StatsChecker statsChecker;
    private final PlayerCache playerCache;

    public ChatHandler(
        MellowOneConfig config,
        NickUtils nickUtils,
        NumberDenicker numberDenicker,
        PregameStats pregameStats,
        PlanckeApi planckeApi,
        StatsChecker statsChecker,
        PlayerCache playerCache
    ) {
        this.config = config;
        this.nickUtils = nickUtils;
        this.numberDenicker = numberDenicker;
        this.pregameStats = pregameStats;
        this.planckeApi = planckeApi;
        this.statsChecker = statsChecker;
        this.playerCache = playerCache;
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        numberDenicker.onChat(event);
        pregameStats.onChat(event);

        String message = event.message.getUnformattedText();
        HypixelFeatures.getInstance().onChat(message);

        processBedwarsUpgradesAndTraps(message);

        if (
            isBedwarsStartMessage(message) ||
            isBedwarsRespawnMessage(message)
        ) {
            BedwarsUpgradesTrapsManager.getInstance().resetUpgradesAndTraps();

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
            new Thread(() -> {
                try {
                    String stats = planckeApi.checkDuels(username);
                    ChatUtils.sendMessage(stats);
                } catch (IOException e) {
                    ChatUtils.sendMessage(
                        "§cFailed to get stats for " + username
                    );
                }
            })
                .start();
        }

        if (
            message.startsWith("The game starts in ") &&
            message.contains(" seconds!")
        ) {
            BedwarsUpgradesTrapsManager.getInstance().resetUpgradesAndTraps();
        }
    }

    private void processBedwarsUpgradesAndTraps(String message) {
        String lower = message.toLowerCase();

        if (lower.contains("purchased")) {
            BedwarsUpgradesTrapsManager.getInstance().processPurchaseMessage(
                message
            );
        }

        if (
            lower.contains("trap was set off!") ||
            lower.contains("reveal trap set off") ||
            (lower.contains("removed") && lower.contains("trap from the queue"))
        ) {
            BedwarsUpgradesTrapsManager.getInstance().processTrapTriggeredMessage(
                message
            );
        }
    }

    private boolean isBedwarsStartMessage(String message) {
        return
            message.contains("Protect your bed and destroy the enemy beds.") &&
            !message.contains(":") &&
            !message.contains("SHOUT");
    }

    private boolean isBedwarsRespawnMessage(String message) {
        return
            message.contains("You will respawn because you still have a bed!") &&
            !message.contains(":") &&
            !message.contains("SHOUT");
    }
}
