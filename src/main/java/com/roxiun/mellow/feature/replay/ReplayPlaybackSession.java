package com.roxiun.mellow.feature.replay;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.util.ChatUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class ReplayPlaybackSession {

    public static final int REPLAY_VIEWER_ENTITY_ID = -310_001;
    private static final int REPLAY_LOCAL_PLAYER_ENTITY_ID = -310_002;
    private static final double[] SPEEDS = new double[] { 0.25D, 0.5D, 1.0D, 2.0D, 4.0D };

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ReplayLoadedData replay;
    private final Runnable stopCallback;

    private ReplayNetworkManager networkManager;
    private ReplayNetHandlerPlayClient netHandler;
    private EntityOtherPlayerMP localReplayPlayer;
    private ReplayScoreboardFrame currentScoreboard;
    private int packetIndex;
    private int chatIndex;
    private int scoreboardIndex;
    private int localSnapshotIndex;
    private int currentTimeMs;
    private boolean paused;
    private boolean viewerPositionInitialized;
    private int speedIndex = 2;
    private int lastControlSlot = 4;
    private String spectatingName = "";

    public ReplayPlaybackSession(ReplayLoadedData replay, Runnable stopCallback) {
        this.replay = replay;
        this.stopCallback = stopCallback;
    }

    public void open() {
        paused = false;
        restartFrom(0, false);
        ChatUtils.sendMessage(
            "§dOpened replay §f" + replay.getMetadata().getReplayId() + "§7. Use the hotbar controls or §f/mreplay§7."
        );
    }

    public void tick() {
        if (!isActive()) {
            return;
        }

        if (mc.thePlayer != null) {
            prepareViewer(mc.thePlayer);
            populateHotbar();
            handleControlSelection();
        }

        if (!paused) {
            currentTimeMs = Math.min(
                replay.getMetadata().getDurationMs(),
                currentTimeMs + (int) Math.round(50.0D * getSpeed())
            );
        }

        applyPacketsUpTo(currentTimeMs);
        emitChatsUpTo(currentTimeMs);
        advanceScoreboardTo(currentTimeMs);
        updateLocalReplayPlayer(currentTimeMs);
        updateSpectateTarget();

        if (!paused && currentTimeMs >= replay.getMetadata().getDurationMs()) {
            paused = true;
            ChatUtils.sendMessage("§7Replay reached the end.");
        }
    }

    public boolean isActive() {
        return netHandler != null;
    }

    public void stop() {
        spectatingName = "";
        localReplayPlayer = null;
        currentScoreboard = null;
        viewerPositionInitialized = false;
        packetIndex = 0;
        chatIndex = 0;
        scoreboardIndex = 0;
        localSnapshotIndex = 0;
        currentTimeMs = 0;
        netHandler = null;
        networkManager = null;
        if (mc.theWorld != null) {
            mc.theWorld.sendQuittingDisconnectingPacket();
        }
        mc.loadWorld(null);
        mc.displayGuiScreen(null);
        if (stopCallback != null) {
            stopCallback.run();
        }
    }

    public void togglePause() {
        paused = !paused;
        ChatUtils.sendMessage(paused ? "§7Replay paused." : "§7Replay resumed.");
    }

    public void changeSpeed(int delta) {
        int next = speedIndex + delta;
        if (next < 0 || next >= SPEEDS.length) {
            return;
        }
        speedIndex = next;
        ChatUtils.sendMessage("§7Replay speed set to §f" + speedLabel() + "§7.");
    }

    public void skipBySeconds(int seconds) {
        seekTo(currentTimeMs + (seconds * 1000));
    }

    public void seekTo(int targetMs) {
        int clamped = Math.max(0, Math.min(replay.getMetadata().getDurationMs(), targetMs));
        boolean wasPaused = paused;
        restartFrom(clamped, true);
        paused = wasPaused;
        ChatUtils.sendMessage("§7Jumped to §f" + formatTime(currentTimeMs) + "§7.");
    }

    public void spectatePlayer(String playerName) {
        if (playerName == null || playerName.trim().isEmpty()) {
            spectatingName = "";
            ChatUtils.sendMessage("§7Stopped spectating replay players.");
            return;
        }
        spectatingName = playerName.trim();
        updateSpectateTarget();
    }

    public ReplayPlaybackState getPlaybackState() {
        if (!isActive()) {
            return ReplayPlaybackState.inactive();
        }
        return new ReplayPlaybackState(
            true,
            paused,
            replay.getMetadata().getReplayId(),
            replay.getMetadata().getMap(),
            replay.getMetadata().getMode(),
            currentTimeMs,
            replay.getMetadata().getDurationMs(),
            getSpeed(),
            spectatingName
        );
    }

    public List<String> buildHudLines() {
        if (!isActive()) {
            return Collections.emptyList();
        }

        List<String> lines = new ArrayList<>();
        lines.add("§5§lReplay");
        lines.add("§7Map: §f" + safe(replay.getMetadata().getMap()));
        lines.add("§7Mode: §f" + safe(replay.getMetadata().getMode()));
        lines.add(
            "§7Time: §f" + formatTime(currentTimeMs) + "§7 / §f" +
            formatTime(replay.getMetadata().getDurationMs())
        );
        lines.add("§7Speed: §f" + speedLabel());
        lines.add("§7State: §f" + (paused ? "Paused" : "Playing"));
        if (!spectatingName.isEmpty()) {
            lines.add("§7Spectating: §f" + spectatingName);
        }
        if (currentScoreboard != null) {
            String title = currentScoreboard.getTitle();
            if (!title.isEmpty()) {
                lines.add(" ");
                lines.add(title);
            }
            List<String> boardLines = currentScoreboard.getLines();
            int limit = Math.min(boardLines.size(), 8);
            for (int i = 0; i < limit; i++) {
                lines.add(boardLines.get(i));
            }
        }
        return lines;
    }

    public void prepareViewer(EntityPlayer viewer) {
        viewer.capabilities.allowFlying = true;
        viewer.capabilities.isFlying = true;
        viewer.capabilities.disableDamage = true;
        viewer.noClip = true;
        viewer.fallDistance = 0.0F;
    }

    public void onJoinGame(S01PacketJoinGame packetIn) {
        localReplayPlayer = null;
        viewerPositionInitialized = false;
    }

    public void onRespawn(S07PacketRespawn packetIn) {
        localReplayPlayer = null;
        viewerPositionInitialized = false;
    }

    public void onViewerPosition(S08PacketPlayerPosLook packetIn) {
        if (mc.thePlayer == null || viewerPositionInitialized) {
            return;
        }
        mc.thePlayer.setPositionAndRotation(
            packetIn.getX(),
            packetIn.getY(),
            packetIn.getZ(),
            packetIn.getYaw(),
            packetIn.getPitch()
        );
        viewerPositionInitialized = true;
    }

    private void restartFrom(int targetMs, boolean announceRebuild) {
        if (mc.theWorld != null) {
            mc.theWorld.sendQuittingDisconnectingPacket();
        }
        mc.loadWorld(null);
        mc.displayGuiScreen(null);
        networkManager = new ReplayNetworkManager();
        netHandler = new ReplayNetHandlerPlayClient(
            mc,
            null,
            networkManager,
            this,
            replay.getMetadata().getViewerName(),
            replay.getMetadata().getViewerUuid()
        );
        packetIndex = 0;
        chatIndex = 0;
        scoreboardIndex = 0;
        localSnapshotIndex = 0;
        currentTimeMs = 0;
        localReplayPlayer = null;
        currentScoreboard = null;
        viewerPositionInitialized = false;
        lastControlSlot = 4;
        applyPacketsUpTo(targetMs);
        advanceScoreboardTo(targetMs);
        updateLocalReplayPlayer(targetMs);
        currentTimeMs = targetMs;
        if (announceRebuild && targetMs > 0) {
            ChatUtils.sendMessage("§7Rebuilt replay state at §f" + formatTime(targetMs) + "§7.");
        }
    }

    private void applyPacketsUpTo(int targetMs) {
        while (packetIndex < replay.getPackets().size()) {
            ReplayPacketFrame frame = replay.getPackets().get(packetIndex);
            if (frame.getTimestampMs() > targetMs) {
                break;
            }
            packetIndex++;
            try {
                Packet<?> packet = ReplayPacketCodec.decode(frame);
                processPacket(packet);
            } catch (Exception ignored) {}
        }
    }

    private void emitChatsUpTo(int targetMs) {
        while (chatIndex < replay.getChats().size()) {
            ReplayChatEvent event = replay.getChats().get(chatIndex);
            if (event.getTimestampMs() > targetMs) {
                break;
            }
            chatIndex++;
            IChatComponent component = IChatComponent.Serializer.jsonToComponent(
                event.getComponentJson()
            );
            if (component == null) {
                component = new ChatComponentText("");
            }
            if (mc.ingameGUI != null) {
                mc.ingameGUI.getChatGUI().printChatMessage(component);
            }
        }
    }

    private void advanceScoreboardTo(int targetMs) {
        while (scoreboardIndex < replay.getScoreboards().size()) {
            ReplayScoreboardFrame frame = replay.getScoreboards().get(scoreboardIndex);
            if (frame.getTimestampMs() > targetMs) {
                break;
            }
            scoreboardIndex++;
            currentScoreboard = frame;
        }
    }

    private void updateLocalReplayPlayer(int targetMs) {
        ReplayLocalPlayerSnapshot snapshot = null;
        while (localSnapshotIndex < replay.getLocalSnapshots().size()) {
            ReplayLocalPlayerSnapshot next = replay.getLocalSnapshots().get(localSnapshotIndex);
            if (next.getTimestampMs() > targetMs) {
                break;
            }
            snapshot = next;
            localSnapshotIndex++;
        }
        if (snapshot == null) {
            if (localSnapshotIndex > 0 && localSnapshotIndex <= replay.getLocalSnapshots().size()) {
                snapshot = replay.getLocalSnapshots().get(localSnapshotIndex - 1);
            } else {
                return;
            }
        }
        if (mc.theWorld == null) {
            return;
        }
        if (localReplayPlayer == null) {
            localReplayPlayer = new EntityOtherPlayerMP(
                mc.theWorld,
                new GameProfile(resolveViewerUuid(), resolveViewerName())
            );
            localReplayPlayer.setEntityId(REPLAY_LOCAL_PLAYER_ENTITY_ID);
            mc.theWorld.addEntityToWorld(REPLAY_LOCAL_PLAYER_ENTITY_ID, localReplayPlayer);
        }
        localReplayPlayer.setSneaking(snapshot.isSneaking());
        localReplayPlayer.setSprinting(snapshot.isSprinting());
        localReplayPlayer.setPositionAndRotation(
            snapshot.getX(),
            snapshot.getY(),
            snapshot.getZ(),
            snapshot.getYaw(),
            snapshot.getPitch()
        );
        localReplayPlayer.rotationYawHead = snapshot.getYaw();
        localReplayPlayer.renderYawOffset = snapshot.getYaw();
        if (!viewerPositionInitialized && mc.thePlayer != null) {
            mc.thePlayer.setPositionAndRotation(
                snapshot.getX(),
                snapshot.getY() + 2.0D,
                snapshot.getZ(),
                snapshot.getYaw(),
                snapshot.getPitch()
            );
            viewerPositionInitialized = true;
        }
    }

    private void updateSpectateTarget() {
        if (spectatingName.isEmpty() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }
        Entity entity = findPlayerEntity(spectatingName);
        if (entity == null) {
            return;
        }
        mc.thePlayer.setPositionAndRotation(
            entity.posX,
            entity.posY + 2.0D,
            entity.posZ,
            entity.rotationYaw,
            entity.rotationPitch
        );
    }

    private Entity findPlayerEntity(String name) {
        if (mc.theWorld == null || name == null) {
            return null;
        }
        for (Object playerObj : mc.theWorld.playerEntities) {
            if (!(playerObj instanceof EntityPlayer)) {
                continue;
            }
            EntityPlayer player = (EntityPlayer) playerObj;
            if (player == mc.thePlayer) {
                continue;
            }
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        if (
            localReplayPlayer != null &&
            localReplayPlayer.getName() != null &&
            localReplayPlayer.getName().equalsIgnoreCase(name)
        ) {
            return localReplayPlayer;
        }
        return null;
    }

    private void handleControlSelection() {
        if (mc.thePlayer == null) {
            return;
        }
        int slot = mc.thePlayer.inventory.currentItem;
        if (slot == lastControlSlot) {
            return;
        }
        lastControlSlot = slot;
        switch (slot) {
            case 2:
                changeSpeed(-1);
                break;
            case 3:
                skipBySeconds(-5);
                break;
            case 4:
                togglePause();
                break;
            case 5:
                skipBySeconds(5);
                break;
            case 6:
                changeSpeed(1);
                break;
            case 8:
                stop();
                return;
            default:
                return;
        }
        if (mc.thePlayer != null) {
            mc.thePlayer.inventory.currentItem = 4;
            lastControlSlot = 4;
        }
    }

    private void populateHotbar() {
        if (mc.thePlayer == null) {
            return;
        }
        mc.thePlayer.inventory.mainInventory[0] = namedItem(Items.book, "§dReplay Info");
        mc.thePlayer.inventory.mainInventory[1] = namedItem(Items.compass, "§d/mellow replay tp <player>");
        mc.thePlayer.inventory.mainInventory[2] = namedItem(Items.redstone, "§dSlow Down");
        mc.thePlayer.inventory.mainInventory[3] = namedItem(Items.arrow, "§dBack 5s");
        mc.thePlayer.inventory.mainInventory[4] = namedItem(
            paused ? Items.slime_ball : Items.nether_star,
            paused ? "§dPlay" : "§dPause"
        );
        mc.thePlayer.inventory.mainInventory[5] = namedItem(Items.arrow, "§dForward 5s");
        mc.thePlayer.inventory.mainInventory[6] = namedItem(Items.sugar, "§dSpeed Up");
        mc.thePlayer.inventory.mainInventory[7] = namedItem(Items.name_tag, "§dSpectate Player");
        mc.thePlayer.inventory.mainInventory[8] = namedItem(Items.bed, "§cExit Replay");
    }

    private ItemStack namedItem(net.minecraft.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.setStackDisplayName(name);
        return stack;
    }

    private double getSpeed() {
        return SPEEDS[speedIndex];
    }

    private String speedLabel() {
        double speed = getSpeed();
        if (speed == (int) speed) {
            return ((int) speed) + "x";
        }
        return String.format(Locale.ROOT, "%.2fx", speed);
    }

    private String formatTime(int timeMs) {
        int totalSeconds = Math.max(0, timeMs / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.ROOT, "%02d:%02d", minutes, seconds);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value;
    }

    private UUID resolveViewerUuid() {
        return replay.getMetadata().getViewerUuid() == null
            ? UUID.nameUUIDFromBytes(
                ("mellow-replay-" + replay.getMetadata().getReplayId()).getBytes()
            )
            : replay.getMetadata().getViewerUuid();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void processPacket(Packet<?> packet) {
        ((Packet) packet).processPacket(netHandler);
    }

    private String resolveViewerName() {
        String name = replay.getMetadata().getViewerName();
        return name == null || name.trim().isEmpty() ? "ReplayPlayer" : name;
    }
}
