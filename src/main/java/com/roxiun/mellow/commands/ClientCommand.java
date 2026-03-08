package com.roxiun.mellow.commands;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.api.seraph.SeraphApi;
import com.roxiun.mellow.api.seraph.SeraphClientType;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class ClientCommand extends CommandBase {

    private final SeraphApi seraphApi;
    private final MojangApi mojangApi;
    private final MellowOneConfig config;

    public ClientCommand(
        SeraphApi seraphApi,
        MojangApi mojangApi,
        MellowOneConfig config
    ) {
        this.seraphApi = seraphApi;
        this.mojangApi = mojangApi;
        this.config = config;
    }

    @Override
    public String getCommandName() {
        return "client";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/client <username>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length != 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use /client <username>"
            );
            return;
        }

        if (
            config == null ||
            config.seraphKey == null ||
            config.seraphKey.trim().isEmpty()
        ) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cSet a Seraph API key in OneConfig first."
            );
            return;
        }

        String username = args[0];
        ChatUtils.sendCommandMessage(
            sender,
            "§7Fetching client for §f" + username + "§7..."
        );

        AsyncExecutor.getInstance().command(() -> {
            UUID uuid = PlayerUtils.resolveLookupUuid(username, mojangApi);
            if (uuid == null) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cPlayer not found: §r" + username
                    )
                );
                return;
            }

            SeraphClientType clientType = seraphApi.fetchClientType(
                uuid.toString(),
                config.seraphKey.trim()
            );

            MainThreadDispatcher.run(() -> {
                if (clientType == null) {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§7No Seraph client data found for §f" + username + "§7."
                    );
                    return;
                }

                ChatUtils.sendCommandMessage(
                    sender,
                    "§f" +
                    username +
                    "§7 is using §b" +
                    clientType.getDisplayName() +
                    "§7."
                );
            });
        });
    }

    @Override
    public java.util.List<String> addTabCompletionOptions(
        ICommandSender sender,
        String[] args,
        BlockPos pos
    ) {
        if (args.length == 1 && Minecraft.getMinecraft().getNetHandler() != null) {
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
