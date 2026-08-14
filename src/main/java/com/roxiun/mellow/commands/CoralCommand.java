package com.roxiun.mellow.commands;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.api.coral.CoralApi;
import com.roxiun.mellow.api.coral.CoralTag;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class CoralCommand extends CommandBase {

    private final CoralApi coralApi;
    private final MellowOneConfig config;

    public CoralCommand(CoralApi coralApi, MellowOneConfig config) {
        this.coralApi = coralApi;
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "coral";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/coral <username>";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("urchin", "murchin");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use /coral <username>"
            );
            return;
        }

        String username = args[0];
        AsyncExecutor.getInstance().command(() -> {
            try {
                List<CoralTag> tags = coralApi.fetchCoralTags(
                    null,
                    username,
                    config.getCoralApiKey()
                );

                if (tags == null || tags.isEmpty()) {
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§aNo Coral tags found for: §r" + username
                        )
                    );
                } else {
                    String formattedTags = FormattingUtils.formatCoralTags(
                        tags
                    );
                    String coralMessage =
                        "§c" + username + " is tagged for: " + formattedTags;
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(sender, coralMessage)
                    );
                }
            } catch (IOException e) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cCould not fetch Coral tags for " +
                            username +
                            ": " +
                            e.getMessage()
                    )
                );
            }
        });
    }

    @Override
    public List<String> addTabCompletionOptions(
        ICommandSender sender,
        String[] args,
        BlockPos pos
    ) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(
                args,
                Minecraft.getMinecraft()
                    .getNetHandler()
                    .getPlayerInfoMap()
                    .stream()
                    .map(NetworkPlayerInfo::getGameProfile)
                    .map(GameProfile::getName)
                    .toArray(String[]::new)
            );
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
