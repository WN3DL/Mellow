package com.roxiun.mellow.feature.replay;

import com.roxiun.mellow.Mellow;
import com.roxiun.mellow.gamestate.GameSnapshot;
import com.roxiun.mellow.util.ChatUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S0CPacketSpawnPlayer;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S43PacketCamera;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.util.IChatComponent;

public class ReplayManager {

    private static final long PREBUFFER_WINDOW_MS = 90_000L;
    private static final int PREBUFFER_MAX_PACKETS = 40_000;
    private static final ReplayManager INSTANCE = new ReplayManager();

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ReplayIo io = new ReplayIo();
    private final List<PendingFrame> pendingFrames = new ArrayList<>();

    private RecordingSession activeRecording;
    private ReplayPlaybackSession activePlayback;
    private GameSnapshot lastSnapshot = GameSnapshot.empty();

    public static ReplayManager getInstance() {
        return INSTANCE;
    }

    private ReplayManager() {}

    public void onGameSnapshot(GameSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        if (isPlaybackActive()) {
            lastSnapshot = snapshot;
            return;
        }
        boolean wasInSession = lastSnapshot != null && lastSnapshot.isInBedwars();
        boolean nowInSession = snapshot.isInBedwars();

        if (!isRecordingEnabled() && activeRecording != null) {
            stopRecording();
        } else if (!wasInSession && nowInSession) {
            startRecording(snapshot);
        } else if (wasInSession && !nowInSession && activeRecording != null) {
            stopRecording();
        }

        if (activeRecording != null) {
            activeRecording.updateSnapshot(snapshot);
        }

        lastSnapshot = snapshot;
    }

    public void onInboundPacket(Packet<?> packet) {
        if (
            packet == null ||
            isPlaybackActive() ||
            !isRecordingEnabled() ||
            shouldSkipPacket(packet)
        ) {
            return;
        }

        long now = System.currentTimeMillis();
        try {
            ReplayPacketFrame frame = ReplayPacketCodec.encode(0, packet);
            if (packet instanceof S01PacketJoinGame && activeRecording != null) {
                stopRecording();
                pendingFrames.clear();
                pendingFrames.add(new PendingFrame(now, frame));
                return;
            }
            if (packet instanceof S01PacketJoinGame) {
                pendingFrames.clear();
            }
            if (activeRecording != null) {
                activeRecording.addPacket(now, frame);
                activeRecording.observeInboundPacket(packet);
            } else {
                pendingFrames.add(new PendingFrame(now, frame));
                trimPendingFrames(now);
            }
        } catch (Exception ignored) {}
    }

    public void onChatReceived(IChatComponent component, byte type) {
        if (component == null || activeRecording == null || !recordChatEnabled()) {
            return;
        }
        activeRecording.addChat(component, type);
    }

    public void onClientTick(GameSnapshot snapshot) {
        if (activeRecording != null) {
            activeRecording.captureTick(snapshot);
        }
        if (activePlayback != null) {
            activePlayback.tick();
        }
    }

    public void onWorldChange() {
        if (!isPlaybackActive()) {
            if (activeRecording != null) {
                stopRecording();
            }
        }
    }

    public boolean isPlaybackActive() {
        return activePlayback != null && activePlayback.isActive();
    }

    public ReplayPlaybackState getPlaybackState() {
        return activePlayback == null
            ? ReplayPlaybackState.inactive()
            : activePlayback.getPlaybackState();
    }

    public List<String> getHudLines() {
        return activePlayback == null
            ? Collections.<String>emptyList()
            : activePlayback.buildHudLines();
    }

    public void stopPlayback() {
        if (activePlayback != null) {
            activePlayback.stop();
            activePlayback = null;
        }
    }

    public void togglePause() {
        if (activePlayback != null) {
            activePlayback.togglePause();
        }
    }

    public void changeSpeed(int delta) {
        if (activePlayback != null) {
            activePlayback.changeSpeed(delta);
        }
    }

    public void seekBySeconds(int seconds) {
        if (activePlayback != null) {
            activePlayback.skipBySeconds(seconds);
        }
    }

    public boolean handlePlaybackControlClick() {
        return activePlayback != null && activePlayback.handleHeldControlClick();
    }

    public void spectatePlayer(String name) {
        if (activePlayback != null) {
            activePlayback.spectatePlayer(name);
        }
    }

    public List<ReplayCatalogEntry> listReplays() {
        return io.listReplays(mc.mcDataDir);
    }

    public boolean openReplay(String token) {
        ReplayCatalogEntry entry = resolveReplayEntry(token);
        if (entry == null) {
            return false;
        }
        try {
            ReplayLoadedData replay = io.loadReplay(entry.getDirectory());
            if (activeRecording != null) {
                stopRecording();
            }
            pendingFrames.clear();
            if (activePlayback != null) {
                activePlayback.stop();
            }
            activePlayback = new ReplayPlaybackSession(
                replay,
                new Runnable() {
                    @Override
                    public void run() {
                        activePlayback = null;
                    }
                }
            );
            activePlayback.open();
            return true;
        } catch (Exception e) {
            ChatUtils.sendMessage("§cFailed to open replay: §f" + e.getMessage());
            return false;
        }
    }

    public boolean deleteReplay(String token) {
        ReplayCatalogEntry entry = resolveReplayEntry(token);
        return entry != null && io.deleteReplay(entry.getDirectory());
    }

    public ReplayCatalogEntry resolveReplayEntry(String token) {
        List<ReplayCatalogEntry> replays = listReplays();
        if (replays.isEmpty()) {
            return null;
        }
        if (token == null || token.trim().isEmpty()) {
            return replays.get(0);
        }
        String trimmed = token.trim();
        try {
            int index = Integer.parseInt(trimmed);
            if (index >= 1 && index <= replays.size()) {
                return replays.get(index - 1);
            }
        } catch (NumberFormatException ignored) {}
        for (ReplayCatalogEntry entry : replays) {
            if (entry.getMetadata().getReplayId().equalsIgnoreCase(trimmed)) {
                return entry;
            }
        }
        return null;
    }

    public void sendReplayList(ICommandSender sender) {
        List<ReplayCatalogEntry> entries = listReplays();
        if (entries.isEmpty()) {
            ChatUtils.sendCommandMessage(sender, "§7No saved replays yet.");
            return;
        }
        ChatUtils.sendCommandMessage(sender, "§d§lMellow Replays");
        int limit = Math.min(entries.size(), 10);
        for (int i = 0; i < limit; i++) {
            ReplayMetadata meta = entries.get(i).getMetadata();
            ChatUtils.sendMultilineCommandMessage(
                sender,
                "§8[" + (i + 1) + "] §f" + meta.getReplayId() +
                " §7- §f" + safe(meta.getMap()) +
                " §7(§f" + safe(meta.getMode()) + "§7)"
            );
        }
    }

    private void startRecording(GameSnapshot snapshot) {
        if (!isRecordingEnabled()) {
            return;
        }
        activeRecording = new RecordingSession(snapshot, pendingFrames);
        ChatUtils.sendMessage(
            "§7Started recording replay for §f" + safe(snapshot.getMap()) + "§7."
        );
    }

    private void stopRecording() {
        RecordingSession session = activeRecording;
        activeRecording = null;
        if (session == null || session.getPackets().isEmpty()) {
            return;
        }
        try {
            File directory = io.createReplayDirectory(mc.mcDataDir, session.getMetadata());
            io.saveReplay(
                directory,
                session.getMetadata(),
                session.getPackets(),
                session.getChats(),
                session.getScoreboards(),
                session.getLocalSnapshots()
            );
            io.pruneOldest(mc.mcDataDir, maxStoredReplays());
            ChatUtils.sendMessage(
                "§7Saved replay §f" + directory.getName() + "§7 (" +
                session.getMetadata().getDurationMs() / 1000 + "s)."
            );
        } catch (Exception e) {
            ChatUtils.sendMessage("§cFailed to save replay: §f" + e.getMessage());
        }
    }

    private void trimPendingFrames(long now) {
        while (pendingFrames.size() > PREBUFFER_MAX_PACKETS) {
            pendingFrames.remove(0);
        }
        while (!pendingFrames.isEmpty()) {
            PendingFrame first = pendingFrames.get(0);
            if (now - first.capturedAt <= PREBUFFER_WINDOW_MS) {
                break;
            }
            pendingFrames.remove(0);
        }
    }

    private boolean isRecordingEnabled() {
        return Mellow.config != null && Mellow.config.enableReplayRecording;
    }

    private boolean recordChatEnabled() {
        return Mellow.config == null || Mellow.config.recordChatInReplays;
    }

    private int maxStoredReplays() {
        return Mellow.config == null ? 0 : Mellow.config.maxStoredReplays;
    }

    private boolean shouldSkipPacket(Packet<?> packet) {
        return
            packet instanceof S02PacketChat ||
            packet instanceof S06PacketUpdateHealth ||
            packet instanceof S09PacketHeldItemChange ||
            packet instanceof S1FPacketSetExperience ||
            packet instanceof S2DPacketOpenWindow ||
            packet instanceof S2EPacketCloseWindow ||
            packet instanceof S2FPacketSetSlot ||
            packet instanceof S30PacketWindowItems ||
            packet instanceof S32PacketConfirmTransaction ||
            packet instanceof S37PacketStatistics ||
            packet instanceof S39PacketPlayerAbilities ||
            packet instanceof S43PacketCamera ||
            packet instanceof S45PacketTitle;
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value;
    }

    private static final class PendingFrame {
        private final long capturedAt;
        private final ReplayPacketFrame frame;

        private PendingFrame(long capturedAt, ReplayPacketFrame frame) {
            this.capturedAt = capturedAt;
            this.frame = frame;
        }
    }

    private final class RecordingSession {

        private final ReplayMetadata metadata = new ReplayMetadata();
        private final List<ReplayPacketFrame> packets = new ArrayList<>();
        private final List<ReplayChatEvent> chats = new ArrayList<>();
        private final List<ReplayScoreboardFrame> scoreboards = new ArrayList<>();
        private final List<ReplayLocalPlayerSnapshot> localSnapshots = new ArrayList<>();
        private final Set<Integer> knownRemotePlayerEntityIds = new HashSet<>();
        private final long baseTime;
        private String lastScoreboardTitle = "";
        private List<String> lastScoreboardLines = Collections.emptyList();
        private ReplayLocalPlayerSnapshot lastLocalSnapshot;

        private RecordingSession(GameSnapshot snapshot, List<PendingFrame> pending) {
            long now = System.currentTimeMillis();
            this.baseTime = pending.isEmpty() ? now : pending.get(0).capturedAt;
            metadata.setStartedAt(baseTime);
            metadata.setViewerName(
                mc.thePlayer == null ? "" : mc.thePlayer.getName()
            );
            metadata.setViewerUuid(
                mc.thePlayer == null ? null : mc.thePlayer.getUniqueID()
            );
            updateSnapshot(snapshot);
            for (PendingFrame frame : pending) {
                addPacket(frame.capturedAt, frame.frame);
                observeStoredFrame(frame.frame);
            }
            pendingFrames.clear();
            captureVisiblePlayers(now);
        }

        private void updateSnapshot(GameSnapshot snapshot) {
            metadata.setMap(snapshot.getMap());
            metadata.setMode(snapshot.getMode());
            metadata.setServerName(snapshot.getServerName());
            metadata.setGameType(
                snapshot.getGameType() == null
                    ? ""
                    : snapshot.getGameType().name().toLowerCase(Locale.ROOT)
            );
        }

        private void addPacket(long capturedAt, ReplayPacketFrame frame) {
            int timestamp = toRelativeTime(capturedAt);
            packets.add(
                new ReplayPacketFrame(timestamp, frame.getClassName(), frame.getPayload())
            );
            metadata.setDurationMs(Math.max(metadata.getDurationMs(), timestamp));
        }

        private void addChat(IChatComponent component, byte type) {
            int timestamp = toRelativeTime(System.currentTimeMillis());
            chats.add(
                new ReplayChatEvent(
                    timestamp,
                    IChatComponent.Serializer.componentToJson(component),
                    type
                )
            );
        }

        private void captureTick(GameSnapshot snapshot) {
            captureVisiblePlayers(System.currentTimeMillis());
            captureScoreboard(snapshot);
            captureLocalPlayerSnapshot();
            metadata.setEndedAt(System.currentTimeMillis());
            metadata.setDurationMs(toRelativeTime(System.currentTimeMillis()));
        }

        private void captureScoreboard(GameSnapshot snapshot) {
            String title = snapshot == null ? "" : snapshot.getScoreboardTitle();
            List<String> lines = snapshot == null
                ? Collections.<String>emptyList()
                : new ArrayList<>(snapshot.getScoreboardLines());
            boolean changed = !safe(title).equals(safe(lastScoreboardTitle)) ||
            !lines.equals(lastScoreboardLines);
            if (!changed) {
                return;
            }
            long now = System.currentTimeMillis();
            scoreboards.add(
                new ReplayScoreboardFrame(toRelativeTime(now), title, lines)
            );
            lastScoreboardTitle = title;
            lastScoreboardLines = lines;
        }

        private void captureLocalPlayerSnapshot() {
            EntityPlayerSP player = mc.thePlayer;
            if (player == null) {
                return;
            }
            ReplayLocalPlayerSnapshot snapshot = new ReplayLocalPlayerSnapshot(
                toRelativeTime(System.currentTimeMillis()),
                player.posX,
                player.posY,
                player.posZ,
                player.rotationYaw,
                player.rotationPitch,
                player.isSneaking(),
                player.isSprinting()
            );
            if (!shouldCaptureLocalSnapshot(snapshot)) {
                return;
            }
            localSnapshots.add(snapshot);
            lastLocalSnapshot = snapshot;
        }

        private boolean shouldCaptureLocalSnapshot(ReplayLocalPlayerSnapshot snapshot) {
            if (lastLocalSnapshot == null) {
                return true;
            }
            return
                Double.compare(snapshot.getX(), lastLocalSnapshot.getX()) != 0 ||
                Double.compare(snapshot.getY(), lastLocalSnapshot.getY()) != 0 ||
                Double.compare(snapshot.getZ(), lastLocalSnapshot.getZ()) != 0 ||
                Float.compare(snapshot.getYaw(), lastLocalSnapshot.getYaw()) != 0 ||
                Float.compare(snapshot.getPitch(), lastLocalSnapshot.getPitch()) != 0 ||
                snapshot.isSneaking() != lastLocalSnapshot.isSneaking() ||
                snapshot.isSprinting() != lastLocalSnapshot.isSprinting();
        }

        private void observeInboundPacket(Packet<?> packet) {
            if (packet instanceof S0CPacketSpawnPlayer) {
                markRemotePlayerEntity(((S0CPacketSpawnPlayer) packet).getEntityID());
            } else if (packet instanceof S13PacketDestroyEntities) {
                forgetRemotePlayerEntities(((S13PacketDestroyEntities) packet).getEntityIDs());
            }
        }

        private void observeStoredFrame(ReplayPacketFrame frame) {
            if (
                !S0CPacketSpawnPlayer.class.getName().equals(frame.getClassName()) &&
                !S13PacketDestroyEntities.class.getName().equals(frame.getClassName())
            ) {
                return;
            }
            try {
                Packet<?> packet = ReplayPacketCodec.decode(frame);
                observeInboundPacket(packet);
            } catch (Exception ignored) {}
        }

        private void captureVisiblePlayers(long capturedAt) {
            if (mc.theWorld == null || mc.thePlayer == null || mc.getNetHandler() == null) {
                return;
            }

            for (Object playerObj : mc.theWorld.playerEntities) {
                if (!(playerObj instanceof EntityPlayer)) {
                    continue;
                }

                EntityPlayer player = (EntityPlayer) playerObj;
                if (player == mc.thePlayer) {
                    continue;
                }

                int entityId = player.getEntityId();
                if (knownRemotePlayerEntityIds.contains(entityId)) {
                    continue;
                }

                if (
                    player.getGameProfile() == null ||
                    player.getGameProfile().getId() == null ||
                    player.getGameProfile().getName() == null ||
                    player.getGameProfile().getName().trim().isEmpty()
                ) {
                    continue;
                }

                NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(
                    player.getUniqueID()
                );
                if (playerInfo == null) {
                    continue;
                }

                try {
                    addPacket(capturedAt, ReplayPacketCodec.encode(0, new S0CPacketSpawnPlayer(player)));
                    markRemotePlayerEntity(entityId);
                } catch (Exception ignored) {}
            }
        }

        private void markRemotePlayerEntity(int entityId) {
            if (mc.thePlayer == null || entityId != mc.thePlayer.getEntityId()) {
                knownRemotePlayerEntityIds.add(entityId);
            }
        }

        private void forgetRemotePlayerEntities(int[] entityIds) {
            if (entityIds == null || entityIds.length == 0) {
                return;
            }
            for (int entityId : entityIds) {
                knownRemotePlayerEntityIds.remove(entityId);
            }
        }

        private int toRelativeTime(long capturedAt) {
            return (int) Math.max(0L, capturedAt - baseTime);
        }

        private ReplayMetadata getMetadata() {
            metadata.setEndedAt(Math.max(metadata.getEndedAt(), System.currentTimeMillis()));
            return metadata;
        }

        private List<ReplayPacketFrame> getPackets() {
            return packets;
        }

        private List<ReplayChatEvent> getChats() {
            return chats;
        }

        private List<ReplayScoreboardFrame> getScoreboards() {
            return scoreboards;
        }

        private List<ReplayLocalPlayerSnapshot> getLocalSnapshots() {
            return localSnapshots;
        }
    }
}
