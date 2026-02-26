package com.roxiun.mellow.util.player;

import com.roxiun.mellow.api.bedwars.BedwarsPlayer;
import com.roxiun.mellow.api.hypixel.HypixelFeatures;
import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.UUIDUtils;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.formatting.FormattingUtils;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientChatReceivedEvent;

public class PregameStats {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final PlayerCache playerCache;
    private final MellowOneConfig config;
    private final BlacklistManager blacklistManager;

    private final Set<String> alreadyLookedUp = ConcurrentHashMap.newKeySet();

    private static final Pattern BEDWARS_CHAT_PATTERN = Pattern.compile(
        "^(?:\\[.*?\\]\\s*)*(\\w{3,16})(?::| ») (.*)$"
    );

    public PregameStats(
        PlayerCache playerCache,
        MellowOneConfig config,
        BlacklistManager blacklistManager
    ) {
        this.playerCache = playerCache;
        this.config = config;
        this.blacklistManager = blacklistManager;
    }

    public void onWorldChange() {
        alreadyLookedUp.clear();
    }

    public void onChat(ClientChatReceivedEvent event) {
        if (!config.pregameStats) {
            return;
        }
        if (!HypixelFeatures.getInstance().getGameContext().isPregameBedwarsLobby()) {
            return;
        }

        String raw = event.message.getUnformattedText();
        String message = raw.replaceAll("§.", "").trim();

        String username = extractUsernameFromChat(message);
        if (username == null) {
            return;
        }

        if (username.equalsIgnoreCase(mc.thePlayer.getName())) {
            return;
        }
        if (!alreadyLookedUp.add(username.toLowerCase())) {
            return;
        }

        AsyncExecutor.getInstance().profileIo(() -> handlePlayer(username));
    }

    private String extractUsernameFromChat(String message) {
        Matcher chatMatch = BEDWARS_CHAT_PATTERN.matcher(message);
        if (chatMatch.find()) {
            return chatMatch.group(1);
        }

        String delimiter = null;
        if (message.contains(" » ")) {
            delimiter = " » ";
        } else if (message.contains(": ")) {
            delimiter = ": ";
        }

        if (delimiter == null) {
            return null;
        }

        String left = message.substring(0, message.indexOf(delimiter)).trim();
        if (left.isEmpty()) {
            return null;
        }

        String[] tokens = left.split("\\s+");
        if (tokens.length == 0) {
            return null;
        }

        String candidate = tokens[tokens.length - 1].replaceAll(
            "[^A-Za-z0-9_]",
            ""
        );
        if (candidate.length() < 3 || candidate.length() > 16) {
            return null;
        }

        return candidate;
    }

    private void handlePlayer(String username) {
        PlayerProfile profile = playerCache.getProfile(username);

        if (profile == null || profile.getBedwarsPlayer() == null) {
            if (config.pregameStats) {
                MainThreadDispatcher.run(() ->
                    ChatUtils.sendMessage(
                        "§cFailed to fetch stats for: §r" +
                            username +
                            " (possibly nicked)"
                    )
                );
            }
            return;
        }

        UUID uuid = UUIDUtils.fromString(profile.getUuid());
        if (blacklistManager.isBlacklisted(uuid)) {
            String reason = blacklistManager
                .getBlacklistedPlayer(uuid)
                .getReason();
            MainThreadDispatcher.run(() -> {
                ChatUtils.sendMessage(
                    "§c" + username + " is on your blacklist: " + reason
                );
                mc.thePlayer.playSound("note.pling", 1.0F, 1.0F);
            });
        }

        if (config.pregameStats) {
            BedwarsPlayer player = profile.getBedwarsPlayer();
            String stats =
                player.getStars() +
                " §r" +
                player.getFormattedNameWithRank() +
                " §7|§r FKDR: " +
                player.getFkdrColor() +
                player.getFormattedFkdr();
            MainThreadDispatcher.run(() -> ChatUtils.sendMessage(stats));
        }

        if (config.urchin && profile.isUrchinTagged()) {
            String tags = FormattingUtils.formatUrchinTags(profile.getUrchinTags());
            String urchinMessage =
                "§c" + username + " is tagged on §5Urchin§c for: " + tags;
            MainThreadDispatcher.run(() -> ChatUtils.sendMessage(urchinMessage));
        }

        if (config.seraph && profile.isSeraphTagged()) {
            String formattedTags = FormattingUtils.formatSeraphTags(
                profile.getSeraphTags()
            );
            String[] tagMessages = formattedTags.split("\\n§c");
            if (tagMessages.length > 0 && !tagMessages[0].trim().isEmpty()) {
                String firstMessage =
                    "§c" + username + " is tagged on §3Seraph§c for: " + tagMessages[0];
                MainThreadDispatcher.run(() -> ChatUtils.sendMessage(firstMessage));
                for (int i = 1; i < tagMessages.length; i++) {
                    if (!tagMessages[i].trim().isEmpty()) {
                        String additionalMessage = "§c" + tagMessages[i];
                        MainThreadDispatcher.run(() ->
                            ChatUtils.sendMessage(additionalMessage)
                        );
                    }
                }
            }
        }
    }
}
