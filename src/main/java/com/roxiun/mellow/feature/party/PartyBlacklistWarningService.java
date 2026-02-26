package com.roxiun.mellow.feature.party;

import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.gamestate.PartyState;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class PartyBlacklistWarningService {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final BlacklistManager blacklistManager;
    private final MellowOneConfig config;
    private final Set<UUID> warnedMembers = new HashSet<>();

    public PartyBlacklistWarningService(
        BlacklistManager blacklistManager,
        MellowOneConfig config
    ) {
        this.blacklistManager = blacklistManager;
        this.config = config;
    }

    public synchronized void onSnapshotUpdate(GameSnapshot snapshot) {
        if (config == null || !config.partyBlacklistWarning) {
            warnedMembers.clear();
            return;
        }

        if (snapshot == null || !snapshot.isOnHypixel()) {
            warnedMembers.clear();
            return;
        }

        PartyState partyState = snapshot.getPartyState();
        if (
            partyState == null ||
            !partyState.isInParty() ||
            partyState.getMembers().isEmpty()
        ) {
            warnedMembers.clear();
            return;
        }

        UUID selfUuid = getSelfUuid();
        Set<UUID> blacklistedInParty = new LinkedHashSet<>();

        for (UUID memberUuid : partyState.getMembers().keySet()) {
            if (memberUuid == null) {
                continue;
            }
            if (selfUuid != null && selfUuid.equals(memberUuid)) {
                continue;
            }
            if (blacklistManager.isBlacklisted(memberUuid)) {
                blacklistedInParty.add(memberUuid);
            }
        }

        warnedMembers.retainAll(blacklistedInParty);
        if (blacklistedInParty.isEmpty()) {
            return;
        }

        Set<UUID> newlyDetected = new LinkedHashSet<>(blacklistedInParty);
        newlyDetected.removeAll(warnedMembers);
        if (newlyDetected.isEmpty()) {
            return;
        }

        warnedMembers.addAll(newlyDetected);
        sendWarning(newlyDetected);
    }

    private void sendWarning(Set<UUID> flaggedMembers) {
        MainThreadDispatcher.run(() -> {
            String names = flaggedMembers
                .stream()
                .map(this::resolveDisplayName)
                .collect(Collectors.joining("§7, §c"));
            String noun = flaggedMembers.size() == 1 ? "member" : "members";

            ChatUtils.sendMessage(
                "§cWarning: blacklisted party " +
                noun +
                " detected: §c" +
                names +
                "§7. Consider leaving to avoid risk."
            );

            if (mc.thePlayer != null) {
                mc.thePlayer.playSound("note.pling", 1.0F, 0.8F);
            }
        });
    }

    private String resolveDisplayName(UUID uuid) {
        String tabName = findTabName(uuid);
        if (tabName != null && !tabName.isEmpty()) {
            if (mc.theWorld != null) {
                return PlayerUtils.getTabDisplayName(tabName);
            }
            return tabName;
        }

        BlacklistedPlayer blacklistedPlayer = blacklistManager.getBlacklistedPlayer(
            uuid
        );
        if (blacklistedPlayer != null) {
            String storedName = blacklistedPlayer.getName();
            if (
                storedName != null &&
                !storedName.trim().isEmpty() &&
                !isUuidLike(storedName)
            ) {
                return storedName;
            }
        }

        return uuid.toString();
    }

    private String findTabName(UUID uuid) {
        if (
            mc.getNetHandler() == null || mc.getNetHandler().getPlayerInfoMap() == null
        ) {
            return null;
        }

        for (NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (
                info != null &&
                info.getGameProfile() != null &&
                uuid.equals(info.getGameProfile().getId())
            ) {
                return info.getGameProfile().getName();
            }
        }

        return null;
    }

    private UUID getSelfUuid() {
        if (
            mc.getSession() == null ||
            mc.getSession().getProfile() == null ||
            mc.getSession().getProfile().getId() == null
        ) {
            return null;
        }
        return mc.getSession().getProfile().getId();
    }

    private boolean isUuidLike(String value) {
        String normalized = value.replace("-", "");
        return normalized.matches("(?i)[0-9a-f]{32}");
    }
}
