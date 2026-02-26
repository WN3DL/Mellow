package com.roxiun.mellow.commands;

import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.gamestate.PartyState;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.scoreboard.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

public class DebugStateCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "mdebug";
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("mellowdebug");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/mdebug <all|state|scoreboard|pregame>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String sub = args.length == 0
            ? "all"
            : args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "all":
                sendState(sender);
                sendScoreboard(sender);
                sendPregame(sender);
                break;
            case "state":
                sendState(sender);
                break;
            case "scoreboard":
            case "board":
                sendScoreboard(sender);
                break;
            case "pregame":
                sendPregame(sender);
                break;
            default:
                ChatUtils.sendCommandMessage(
                    sender,
                    "§cUsage: /mdebug <all|state|scoreboard|pregame>"
                );
                break;
        }
    }

    private void sendState(ICommandSender sender) {
        HypixelFeatures features = HypixelFeatures.getInstance();
        features.onClientTick();

        GameSnapshot snapshot = features.getGameSnapshot();
        PartyState party = features.getPartyState();

        long updatedAt = snapshot.getUpdatedAt();
        long ageMs = updatedAt <= 0 ? -1 : System.currentTimeMillis() - updatedAt;

        ChatUtils.sendCommandMessage(sender, "§d§lMellow Debug §7(State)");
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Hypixel detected: §f" + HypixelUtils.INSTANCE.isHypixel()
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7On Hypixel snapshot: §f" + snapshot.isOnHypixel()
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7GameType: §f" + String.valueOf(snapshot.getGameType())
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Server: §f" + safe(snapshot.getServerName())
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Mode: §f" + safe(snapshot.getMode())
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Map: §f" + safe(snapshot.getMap())
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Lobby: §f" + snapshot.isLobby() + " §7| Pregame: §f" + snapshot.isPregame()
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Bedwars session: §f" + features.isInBedwarsSession() +
            " §7| Bedwars match: §f" + features.isInBedwars()
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Location update age: §f" + (ageMs < 0 ? "never" : (ageMs + "ms"))
        );

        int partySize = party.getMembers() == null ? 0 : party.getMembers().size();
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Party inParty: §f" + party.isInParty() +
            " §7| members: §f" + partySize
        );

        UUID leader = party.getLeader();
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Party leader UUID: §f" + (leader == null ? "none" : leader.toString())
        );
    }

    private void sendScoreboard(ICommandSender sender) {
        List<String> lines = ScoreboardUtils.getSidebarLines();
        String title = ScoreboardUtils.getSidebarTitle();

        ChatUtils.sendCommandMessage(sender, "§d§lMellow Debug §7(Scoreboard)");
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Title: §f" + (title.isEmpty() ? "<empty>" : title)
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Lines: §f" + lines.size()
        );

        if (lines.isEmpty()) {
            ChatUtils.sendMultilineCommandMessage(sender, "§7(no sidebar lines)");
            return;
        }

        int limit = Math.min(lines.size(), 15);
        for (int i = 0; i < limit; i++) {
            ChatUtils.sendMultilineCommandMessage(
                sender,
                "§8[" + i + "] §f" + lines.get(i)
            );
        }
    }

    private void sendPregame(ICommandSender sender) {
        HypixelFeatures features = HypixelFeatures.getInstance();
        features.onClientTick();

        List<String> lines = ScoreboardUtils.getSidebarLines();
        List<String> playerLines = new ArrayList<>();

        for (String line : lines) {
            String normalized = line.toLowerCase(Locale.ROOT).trim();
            if (normalized.startsWith("players:")) {
                playerLines.add(line);
            }
        }

        ChatUtils.sendCommandMessage(sender, "§d§lMellow Debug §7(Pregame)");
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Config pregameStats: §f" + (Mellow.config != null && Mellow.config.pregameStats)
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Hypixel detected: §f" + HypixelUtils.INSTANCE.isHypixel()
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7isInPregameLobby(): §f" + features.isInPregameLobby()
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7isInBedwarsSession(): §f" + features.isInBedwarsSession()
        );
        ChatUtils.sendMultilineCommandMessage(
            sender,
            "§7Players: lines found: §f" + playerLines.size()
        );

        if (playerLines.isEmpty()) {
            ChatUtils.sendMultilineCommandMessage(
                sender,
                "§cNo scoreboard line starts with 'Players:'"
            );
            ChatUtils.sendMultilineCommandMessage(
                sender,
                "§7Tip: if your scoreboard says 'Players' without colon, pregame will currently be false."
            );
        } else {
            for (String line : playerLines) {
                ChatUtils.sendMultilineCommandMessage(
                    sender,
                    "§7Players line: §f" + line
                );
            }
        }
    }

    private String safe(String value) {
        return (value == null || value.isEmpty()) ? "<empty>" : value;
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
                "all",
                "state",
                "scoreboard",
                "pregame"
            );
        }
        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
