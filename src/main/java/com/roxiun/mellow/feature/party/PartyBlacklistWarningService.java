package com.roxiun.mellow.feature.party;

import com.roxiun.mellow.cache.PlayerCache;
import com.roxiun.mellow.config.MellowOneConfig;
import com.roxiun.mellow.core.async.AsyncExecutor;
import com.roxiun.mellow.core.async.MainThreadDispatcher;
import com.roxiun.mellow.data.PlayerProfile;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.gamestate.PartyState;
import com.roxiun.mellow.util.ChatUtils;
import com.roxiun.mellow.util.blacklist.BlacklistManager;
import com.roxiun.mellow.util.blacklist.BlacklistedPlayer;
import com.roxiun.mellow.util.player.PlayerUtils;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;

public class PartyBlacklistWarningService {

    private enum FlagSource {
        LOCAL("§cLocal"),
        URCHIN("§5Urchin"),
        SERAPH("§3Seraph");

        private final String coloredLabel;

        FlagSource(String coloredLabel) {
            this.coloredLabel = coloredLabel;
        }
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final BlacklistManager blacklistManager;
    private final MellowOneConfig config;
    private final PlayerCache playerCache;
    private final Map<UUID, EnumSet<FlagSource>> warnedSourcesByMember =
        new HashMap<>();
    private long evaluationVersion;

    public PartyBlacklistWarningService(
        BlacklistManager blacklistManager,
        MellowOneConfig config,
        PlayerCache playerCache
    ) {
        this.blacklistManager = blacklistManager;
        this.config = config;
        this.playerCache = playerCache;
    }

    public synchronized void onSnapshotUpdate(GameSnapshot snapshot) {
        if (config == null || !config.partyBlacklistWarning) {
            resetState();
            return;
        }

        if (snapshot == null || !snapshot.isOnHypixel()) {
            resetState();
            return;
        }

        PartyState partyState = snapshot.getPartyState();
        if (
            partyState == null ||
            !partyState.isInParty() ||
            partyState.getMembers().isEmpty()
        ) {
            resetState();
            return;
        }

        UUID selfUuid = getSelfUuid();
        Set<UUID> partyMemberUuids = new LinkedHashSet<>();
        Map<UUID, String> tabNamesByUuid = new LinkedHashMap<>();
        Map<UUID, EnumSet<FlagSource>> localFlags = new LinkedHashMap<>();

        for (UUID memberUuid : partyState.getMembers().keySet()) {
            if (memberUuid == null) {
                continue;
            }
            if (selfUuid != null && selfUuid.equals(memberUuid)) {
                continue;
            }

            partyMemberUuids.add(memberUuid);
            String tabName = findTabName(memberUuid);
            if (tabName != null && !tabName.isEmpty()) {
                tabNamesByUuid.put(memberUuid, tabName);
            }

            if (blacklistManager.isBlacklisted(memberUuid)) {
                EnumSet<FlagSource> localSource = EnumSet.of(FlagSource.LOCAL);
                localFlags.put(memberUuid, localSource);
            }
        }

        if (partyMemberUuids.isEmpty()) {
            resetState();
            return;
        }

        boolean shouldCheckUrchin = config.urchin;
        boolean shouldCheckSeraph = config.seraph;
        long evaluationId = ++evaluationVersion;

        if ((!shouldCheckUrchin && !shouldCheckSeraph) || playerCache == null) {
            applyDetectionResult(evaluationId, localFlags);
            return;
        }

        AsyncExecutor.getInstance().profileIo(() -> {
            Map<UUID, EnumSet<FlagSource>> combined = cloneFlagMap(localFlags);

            for (UUID memberUuid : partyMemberUuids) {
                String tabName = tabNamesByUuid.get(memberUuid);
                if (tabName == null || tabName.isEmpty()) {
                    continue;
                }

                PlayerProfile profile = playerCache.getProfile(tabName);
                if (profile == null) {
                    continue;
                }

                EnumSet<FlagSource> sources = combined.get(memberUuid);
                if (sources == null) {
                    sources = EnumSet.noneOf(FlagSource.class);
                }

                if (shouldCheckUrchin && profile.isUrchinTagged()) {
                    sources.add(FlagSource.URCHIN);
                }
                if (shouldCheckSeraph && profile.isSeraphTagged()) {
                    sources.add(FlagSource.SERAPH);
                }

                if (sources.isEmpty()) {
                    combined.remove(memberUuid);
                } else {
                    combined.put(memberUuid, sources);
                }
            }

            MainThreadDispatcher.run(() ->
                applyDetectionResult(evaluationId, combined)
            );
        });
    }

    private synchronized void resetState() {
        evaluationVersion++;
        warnedSourcesByMember.clear();
    }

    private synchronized void applyDetectionResult(
        long evaluationId,
        Map<UUID, EnumSet<FlagSource>> detectedSourcesByMember
    ) {
        if (evaluationId != evaluationVersion) {
            return;
        }

        warnedSourcesByMember.keySet().retainAll(detectedSourcesByMember.keySet());
        Map<UUID, EnumSet<FlagSource>> changed = new LinkedHashMap<>();

        for (Map.Entry<UUID, EnumSet<FlagSource>> entry : detectedSourcesByMember.entrySet()) {
            UUID memberUuid = entry.getKey();
            EnumSet<FlagSource> current = EnumSet.copyOf(entry.getValue());
            EnumSet<FlagSource> previous = warnedSourcesByMember.get(memberUuid);

            if (previous == null || !previous.equals(current)) {
                changed.put(memberUuid, current);
            }

            warnedSourcesByMember.put(memberUuid, current);
        }

        if (changed.isEmpty()) {
            return;
        }

        sendWarning(changed);
    }

    private Map<UUID, EnumSet<FlagSource>> cloneFlagMap(
        Map<UUID, EnumSet<FlagSource>> source
    ) {
        Map<UUID, EnumSet<FlagSource>> copy = new HashMap<>();
        for (Map.Entry<UUID, EnumSet<FlagSource>> entry : source.entrySet()) {
            copy.put(entry.getKey(), EnumSet.copyOf(entry.getValue()));
        }
        return copy;
    }

    private void sendWarning(Map<UUID, EnumSet<FlagSource>> flaggedMembers) {
        MainThreadDispatcher.run(() -> {
            List<String> parts = new ArrayList<>(flaggedMembers.size());
            for (Map.Entry<UUID, EnumSet<FlagSource>> entry : flaggedMembers.entrySet()) {
                String displayName = resolveDisplayName(entry.getKey());
                String labels = formatSourceLabels(entry.getValue());
                parts.add(displayName + " §7[" + labels + "§7]");
            }

            String names = String.join("§7, ", parts);
            String noun = flaggedMembers.size() == 1 ? "member" : "members";

            ChatUtils.sendMessage(
                "§cWarning: flagged party " +
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

    private String formatSourceLabels(EnumSet<FlagSource> sources) {
        List<String> labels = new ArrayList<>(3);
        if (sources.contains(FlagSource.LOCAL)) {
            labels.add(FlagSource.LOCAL.coloredLabel);
        }
        if (sources.contains(FlagSource.URCHIN)) {
            labels.add(FlagSource.URCHIN.coloredLabel);
        }
        if (sources.contains(FlagSource.SERAPH)) {
            labels.add(FlagSource.SERAPH.coloredLabel);
        }
        return String.join("§7, ", labels);
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
