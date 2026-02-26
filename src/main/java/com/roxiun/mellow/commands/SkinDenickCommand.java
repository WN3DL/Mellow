package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import com.roxiun.mellow.util.skins.SkinUtils;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class SkinDenickCommand extends CommandBase {

    private final PlayerCache playerCache;

    public SkinDenickCommand(PlayerCache playerCache) {
        this.playerCache = playerCache;
    }

    @Override
    public String getCommandName() {
        return "skindenick";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/skindenick <player>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cUsage: " + getCommandUsage(sender)
            );
            return;
        }

        String playerName = args[0];
        NetworkPlayerInfo playerInfo = null;

        for (NetworkPlayerInfo info : Minecraft.getMinecraft()
            .getNetHandler()
            .getPlayerInfoMap()) {
            if (info.getGameProfile().getName().equalsIgnoreCase(playerName)) {
                playerInfo = info;
                break;
            }
        }

        if (playerInfo == null) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cPlayer not found: " + playerName
            );
            return;
        }

        String realName = SkinUtils.getRealName(playerInfo);

        if (realName != null) {
            String nickedPlayerDisplay = FormattingUtils.formatNickedPlayerName(
                playerName
            );
            ChatUtils.sendCommandMessage(
                sender,
                "§7Fetching stats for §a" + realName + "§7..."
            );

            new Thread(() -> {
                PlayerProfile profile = playerCache.getProfile(realName);
                String resolvedDisplay = "§a" + realName;
                String starsPrefix = "";

                if (profile != null && profile.getBedwarsPlayer() != null) {
                    BedwarsPlayer bwPlayer = profile.getBedwarsPlayer();
                    resolvedDisplay = bwPlayer.getFormattedNameWithRank();
                    starsPrefix = bwPlayer.getStars() + " §r";
                }

                final String finalResolvedDisplay = resolvedDisplay;
                final String finalStarsPrefix = starsPrefix;
                Minecraft.getMinecraft().addScheduledTask(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        nickedPlayerDisplay +
                            " §d> §r" +
                            finalStarsPrefix +
                            finalResolvedDisplay
                    )
                );
            })
                .start();
        } else {
            ChatUtils.sendCommandMessage(
                sender,
                "§cCould not retrieve the real name for " +
                    playerName +
                    ". They might be using a default skin."
            );
        }
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public List<String> addTabCompletionOptions(
        ICommandSender sender,
        String[] args,
        net.minecraft.util.BlockPos pos
    ) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Minecraft.getMinecraft()
                    .getNetHandler()
                    .getPlayerInfoMap()
                    .stream()
                    .map(info -> info.getGameProfile().getName())
                    .toArray(String[]::new)
            );
        }
        return null;
    }
}
