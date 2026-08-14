package com.roxiun.mellow.commands;

import com.roxiun.mellow.feature.stats.InGameTabStatsSyncService;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.util.ChatUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class RefreshCommand extends CommandBase {

    private final InGameTabStatsSyncService syncService;

    public RefreshCommand(InGameTabStatsSyncService syncService) {
        this.syncService = syncService;
    }

    @Override
    public String getCommandName() {
        return "refresh";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/refresh";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        GameSnapshot snapshot = syncService.getCurrentSnapshot();
        if (snapshot == null || !syncService.isSupportedMatch(snapshot)) {
            ChatUtils.sendCommandMessage(
                sender,
                "§c/refresh can only be used during a supported live match."
            );
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer != null) {
            mc.thePlayer.sendChatMessage("/who");
        }
        syncService.forceRefresh();
        ChatUtils.sendCommandMessage(
            sender,
            "§aRefreshing stats for all visible players..."
        );
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
