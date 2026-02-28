package com.roxiun.mellow.commands;

import com.roxiun.mellow.api.mojang.MojangApi;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.annoylist.AnnoylistManager;
import com.roxiun.mellow.util.annoylist.AnnoylistedPlayer;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

public class AnnoylistCommand extends CommandBase {

    private final AnnoylistManager annoylistManager;
    private final MojangApi mojangApi;
    private static final String BASE_COMMAND = "annoylist";

    public AnnoylistCommand(AnnoylistManager annoylistManager, MojangApi mojangApi) {
        this.annoylistManager = annoylistManager;
        this.mojangApi = mojangApi;
    }

    @Override
    public String getCommandName() {
        return BASE_COMMAND;
    }

    @Override
    public List<String> getCommandAliases() {
        return Arrays.asList("annoy");
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/" + BASE_COMMAND + " <add | remove | list | import>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length < 1) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use " + getCommandUsage(sender)
            );
            return;
        }

        String subCommand = args[0];

        if ("list".equalsIgnoreCase(subCommand)) {
            Map<UUID, AnnoylistedPlayer> annoylist = annoylistManager.getAnnoylist();
            if (annoylist.isEmpty()) {
                ChatUtils.sendCommandMessage(
                    sender,
                    "§aThe annoy list is empty."
                );
                return;
            }

            int page = 1;
            int pageSize = 10;

            if (args.length > 1) {
                try {
                    page = Integer.parseInt(args[1]);
                    if (page < 1) {
                        page = 1;
                    }
                } catch (NumberFormatException e) {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cInvalid page number. Using page 1."
                    );
                }
            }

            List<AnnoylistedPlayer> players = new java.util.ArrayList<>(
                annoylist.values()
            );
            int totalPlayers = players.size();
            int totalPages = (int) Math.ceil((double) totalPlayers / pageSize);

            if (page > totalPages) {
                page = totalPages;
                if (totalPages == 0) {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§aThe annoy list is empty."
                    );
                    return;
                }
            }

            int startIndex = (page - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, totalPlayers);

            ChatUtils.sendCommandMessage(
                sender,
                "§aPlayers on your annoy list (Page " + page + "/" + totalPages + "):"
            );
            for (int i = startIndex; i < endIndex; i++) {
                AnnoylistedPlayer player = players.get(i);
                sender.addChatMessage(
                    new ChatComponentText(
                        "§r- " + player.getName() + ": " + player.getReason()
                    )
                );
            }

            if (totalPages > 1) {
                String navigationMessage =
                    "§7Use §f/" +
                    BASE_COMMAND +
                    " list <page>§7 to navigate";
                if (page < totalPages) {
                    navigationMessage +=
                        " (Next: §f/" +
                        BASE_COMMAND +
                        " list " +
                        (page + 1) +
                        "§7)";
                }
                ChatUtils.sendCommandMessage(sender, navigationMessage);
            }
            return;
        } else if ("import".equalsIgnoreCase(subCommand)) {
            if (args.length < 2) {
                ChatUtils.sendCommandMessage(
                    sender,
                    "§cUsage: /" + BASE_COMMAND + " import <filename>"
                );
                return;
            }

            String filename = args[1];
            File syncFile;
            if (filename.startsWith("/")) {
                syncFile = new File(filename);
            } else {
                File configDir = new File(
                    Minecraft.getMinecraft().mcDataDir,
                    "config/mellow"
                );
                syncFile = new File(configDir, filename);
            }

            AsyncExecutor.getInstance().command(() -> {
                MainThreadDispatcher.run(() -> {
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§eImporting annoy list with file: " +
                            syncFile.getAbsolutePath()
                    );
                });

                try {
                    int newEntries = annoylistManager.syncWithExternalFile(syncFile);
                    String message;
                    if (newEntries > 0) {
                        message =
                            "§aSuccessfully imported! Added " +
                            newEntries +
                            " new entries from " +
                            filename;
                    } else if (syncFile.exists()) {
                        message =
                            "§eImport completed! No new entries found in " + filename;
                    } else {
                        message =
                            "§cFile not found: " + syncFile.getAbsolutePath();
                        MainThreadDispatcher.run(() ->
                            ChatUtils.sendCommandMessage(sender, message)
                        );
                        return;
                    }

                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(sender, message)
                    );
                } catch (Exception e) {
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§cError importing file: " + e.getMessage()
                        )
                    );
                }
            });
            return;
        }

        if (args.length < 2) {
            ChatUtils.sendCommandMessage(
                sender,
                "§cInvalid usage! Use " + getCommandUsage(sender)
            );
            return;
        }

        String playerName = args[1];

        AsyncExecutor.getInstance().command(() -> {
            String uuidString = mojangApi.getUUIDFromName(playerName);
            if (uuidString == null) {
                uuidString = mojangApi.fetchUUID(playerName);
            }

            if (uuidString == null || uuidString.equals("ERROR")) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cCould not find player: " + playerName
                    )
                );
                return;
            }

            UUID uuid = UUIDUtils.fromString(uuidString);

            if ("add".equalsIgnoreCase(subCommand)) {
                String reason;
                if (args.length < 3) {
                    reason = "(none)";
                } else {
                    reason = String.join(
                        " ",
                        Arrays.copyOfRange(args, 2, args.length)
                    );
                }

                boolean playerAdded = annoylistManager.addPlayer(
                    uuid,
                    playerName,
                    reason
                );
                if (playerAdded) {
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§aAdded " + playerName + " to the annoy list."
                        )
                    );
                } else {
                    MainThreadDispatcher.run(() ->
                        ChatUtils.sendCommandMessage(
                            sender,
                            "§c" +
                                playerName +
                                " is already on the annoy list for reason: " +
                                annoylistManager
                                    .getAnnoylistedPlayer(uuid)
                                    .getReason()
                        )
                    );
                }
            } else if ("remove".equalsIgnoreCase(subCommand)) {
                annoylistManager.removePlayer(uuid);
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§aRemoved " + playerName + " from the annoy list."
                    )
                );
            } else {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendCommandMessage(
                        sender,
                        "§cInvalid subcommand! Use 'add', 'remove', or 'list'."
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
                "add",
                "remove",
                "list",
                "import"
            );
        }

        if (args.length == 2 && "list".equalsIgnoreCase(args[0])) {
            List<String> numbers = new java.util.ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                numbers.add(String.valueOf(i));
            }
            return getListOfStringsMatchingLastWord(
                args,
                numbers.toArray(new String[0])
            );
        }

        if (
            args.length == 2 &&
            ("add".equalsIgnoreCase(args[0]) ||
                "remove".equalsIgnoreCase(args[0]))
        ) {
            return null;
        }

        if (args.length == 2 && "import".equalsIgnoreCase(args[0])) {
            return null;
        }

        return null;
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
}
