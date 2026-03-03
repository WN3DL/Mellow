package com.roxiun.mellow.commands;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.urchin.UrchinApi;
import com.roxiun.mellow.api.urchin.UrchinTag;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.blacklist.BlacklistCommandResolver;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class UrchinCommand extends CommandBase {

    private final UrchinApi urchinApi;
    private final MojangApi mojangApi;
    private final MellowOneConfig config;

    public UrchinCommand(
        UrchinApi urchinApi,
        MojangApi mojangApi,
        MellowOneConfig config
    ) {
        this.urchinApi = urchinApi;
        this.mojangApi = mojangApi;
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "urchin";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/urchin <username>";
    }

    @Override
    public List<String> getCommandAliases() {
        if (!BlacklistCommandResolver.isSeraphLoaded()) {
            return Collections.emptyList();
        }
        return Arrays.asList("murchin");
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use /urchin <username>"
            );
            return;
        }

        String username = args[0];
        AsyncExecutor.getInstance().command(() -> {
            try {
                String uuid = mojangApi.fetchUUID(username);
                if (uuid == null || uuid.isEmpty()) {
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§cCould not find UUID for: §r" + username
                        )
                    );
                    return;
                }

                List<UrchinTag> tags = urchinApi.fetchUrchinTags(
                    uuid,
                    username,
                    config.urchinKey
                );

                if (tags == null || tags.isEmpty()) {
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§aNo Urchin tags found for: §r" + username
                        )
                    );
                } else {
                    String formattedTags = FormattingUtils.formatUrchinTags(
                        tags
                    );
                    String urchinMessage =
                        "§c" + username + " is tagged for: " + formattedTags;
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(sender, urchinMessage)
                    );
                }
            } catch (IOException e) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cAn error occurred while fetching Urchin tags for " +
                            username +
                            "."
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
