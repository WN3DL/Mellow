package com.roxiun.mellow.feature.replay;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.mixin.replay.NetHandlerPlayClientAccessor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S19PacketEntityHeadLook;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.network.play.server.S38PacketPlayerListItem;
import net.minecraft.network.play.server.S39PacketPlayerAbilities;
import net.minecraft.network.play.server.S43PacketCamera;
import net.minecraft.network.play.server.S45PacketTitle;
import net.minecraft.profiler.Profiler;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

public class ReplayNetHandlerPlayClient extends NetHandlerPlayClient {

    private final Minecraft mc;
    private final ReplayPlaybackSession session;
    private final Deque<String> fallbackNames = new ArrayDeque<>();
    private final Set<String> claimedNames = new HashSet<>();
    private int fallbackCounter = 1;

    public ReplayNetHandlerPlayClient(
        Minecraft mc,
        GuiScreen parentScreen,
        NetworkManager networkManager,
        ReplayPlaybackSession session,
        String viewerName,
        UUID viewerUuid
    ) {
        super(
            mc,
            parentScreen,
            networkManager,
            new GameProfile(
                viewerUuid == null ? UUID.randomUUID() : viewerUuid,
                viewerName == null || viewerName.trim().isEmpty()
                    ? "ReplayViewer"
                    : viewerName
            )
        );
        this.mc = mc;
        this.session = session;
    }

    @Override
    public void handleJoinGame(S01PacketJoinGame packetIn) {
        if (!session.shouldProcessWorldBootstrapPacket()) {
            return;
        }
        WorldType worldType = packetIn.getWorldType() == null
            ? WorldType.DEFAULT
            : packetIn.getWorldType();
        EnumDifficulty difficulty = packetIn.getDifficulty() == null
            ? EnumDifficulty.NORMAL
            : packetIn.getDifficulty();
        PlayerControllerMP controller = new PlayerControllerMP(mc, this);
        mc.playerController = controller;
        WorldClient world = new WorldClient(
            this,
            new WorldSettings(
                0L,
                WorldSettings.GameType.CREATIVE,
                false,
                packetIn.isHardcoreMode(),
                worldType
            ),
            packetIn.getDimension(),
            difficulty,
            resolveProfiler(mc)
        );
        ((NetHandlerPlayClientAccessor) this).mellow$setClientWorldController(world);
        ((NetHandlerPlayClientAccessor) this).mellow$setDoneLoadingTerrain(true);
        mc.gameSettings.difficulty = difficulty;
        mc.loadWorld(world, "Loading replay");
        if (mc.thePlayer != null) {
            mc.thePlayer.dimension = packetIn.getDimension();
            mc.thePlayer.setEntityId(ReplayPlaybackSession.REPLAY_VIEWER_ENTITY_ID);
            controller.setGameType(WorldSettings.GameType.CREATIVE);
            controller.flipPlayer(mc.thePlayer);
            session.prepareViewer(mc.thePlayer);
        }
        mc.displayGuiScreen(null);
        session.onJoinGame(packetIn);
    }

    @Override
    public void handleRespawn(S07PacketRespawn packetIn) {
        if (!session.shouldProcessWorldBootstrapPacket()) {
            return;
        }
        WorldType worldType = packetIn.getWorldType() == null
            ? WorldType.DEFAULT
            : packetIn.getWorldType();
        EnumDifficulty difficulty = packetIn.getDifficulty() == null
            ? EnumDifficulty.NORMAL
            : packetIn.getDifficulty();
        WorldClient world = new WorldClient(
            this,
            new WorldSettings(
                0L,
                WorldSettings.GameType.CREATIVE,
                false,
                false,
                worldType
            ),
            packetIn.getDimensionID(),
            difficulty,
            resolveProfiler(mc)
        );
        ((NetHandlerPlayClientAccessor) this).mellow$setClientWorldController(world);
        mc.gameSettings.difficulty = difficulty;
        mc.loadWorld(world, "Loading replay");
        if (mc.thePlayer != null) {
            mc.thePlayer.dimension = packetIn.getDimensionID();
            mc.thePlayer.setEntityId(ReplayPlaybackSession.REPLAY_VIEWER_ENTITY_ID);
            if (mc.playerController != null) {
                mc.playerController.setGameType(WorldSettings.GameType.CREATIVE);
                mc.playerController.flipPlayer(mc.thePlayer);
            }
            session.prepareViewer(mc.thePlayer);
        }
        mc.displayGuiScreen(null);
        session.onRespawn(packetIn);
    }

    @Override
    public void handlePlayerPosLook(S08PacketPlayerPosLook packetIn) {
        session.onViewerPosition(packetIn);
    }

    @Override
    public void handleEntityMovement(S14PacketEntity packetIn) {
        Entity entity = packetIn.getEntity(mc.theWorld);
        if (entity == null) {
            return;
        }
        super.handleEntityMovement(packetIn);
    }

    @Override
    public void handleEntityTeleport(S18PacketEntityTeleport packetIn) {
        ensureReplayPlayer(
            packetIn.getEntityId(),
            packetIn.getX() / 32.0D,
            packetIn.getY() / 32.0D,
            packetIn.getZ() / 32.0D,
            (packetIn.getYaw() * 360.0F) / 256.0F,
            (packetIn.getPitch() * 360.0F) / 256.0F
        );
        super.handleEntityTeleport(packetIn);
    }

    @Override
    public void handleEntityHeadLook(S19PacketEntityHeadLook packetIn) {
        if (packetIn.getEntity(mc.theWorld) != null) {
            super.handleEntityHeadLook(packetIn);
        }
    }

    @Override
    public void handlePlayerListItem(S38PacketPlayerListItem packetIn) {
        if (packetIn.getAction() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
            for (S38PacketPlayerListItem.AddPlayerData entry : packetIn.getEntries()) {
                if (entry == null || entry.getProfile() == null) {
                    continue;
                }
                String name = entry.getProfile().getName();
                if (name == null || name.trim().isEmpty()) {
                    continue;
                }
                if (mc.thePlayer != null && name.equalsIgnoreCase(mc.thePlayer.getName())) {
                    continue;
                }
                if (!claimedNames.contains(name)) {
                    fallbackNames.add(name);
                }
            }
        }
        super.handlePlayerListItem(packetIn);
    }

    @Override
    public void handleChat(S02PacketChat packetIn) {}

    @Override
    public void handleUpdateHealth(S06PacketUpdateHealth packetIn) {}

    @Override
    public void handleHeldItemChange(S09PacketHeldItemChange packetIn) {}

    @Override
    public void handleSetExperience(S1FPacketSetExperience packetIn) {}

    @Override
    public void handlePlayerAbilities(S39PacketPlayerAbilities packetIn) {}

    @Override
    public void handleOpenWindow(S2DPacketOpenWindow packetIn) {}

    @Override
    public void handleCloseWindow(S2EPacketCloseWindow packetIn) {}

    @Override
    public void handleSetSlot(S2FPacketSetSlot packetIn) {}

    @Override
    public void handleWindowItems(S30PacketWindowItems packetIn) {}

    @Override
    public void handleConfirmTransaction(S32PacketConfirmTransaction packetIn) {}

    @Override
    public void handleStatistics(S37PacketStatistics packetIn) {}

    @Override
    public void handleCamera(S43PacketCamera packetIn) {}

    @Override
    public void handleTitle(S45PacketTitle packetIn) {}

    @Override
    public void handleChangeGameState(S2BPacketChangeGameState packetIn) {
        int state = packetIn.getGameState();
        if (state == 1 || state == 2 || state == 7 || state == 8) {
            super.handleChangeGameState(packetIn);
        }
    }

    private static Profiler resolveProfiler(Minecraft minecraft) {
        return minecraft == null ? new Profiler() : minecraft.mcProfiler;
    }

    private void ensureReplayPlayer(
        int entityId,
        double x,
        double y,
        double z,
        float yaw,
        float pitch
    ) {
        if (mc.theWorld == null) {
            return;
        }
        if (
            entityId == ReplayPlaybackSession.REPLAY_VIEWER_ENTITY_ID ||
            (mc.thePlayer != null && entityId == mc.thePlayer.getEntityId())
        ) {
            return;
        }
        Entity existing = mc.theWorld.getEntityByID(entityId);
        if (existing != null) {
            return;
        }

        String name = nextFallbackName();
        EntityOtherPlayerMP player = new EntityOtherPlayerMP(
            mc.theWorld,
            new GameProfile(
                UUID.nameUUIDFromBytes(
                    ("mellow-replay-fallback-" + entityId).getBytes(StandardCharsets.UTF_8)
                ),
                name
            )
        );
        player.setEntityId(entityId);
        player.setPositionAndRotation(x, y, z, yaw, pitch);
        mc.theWorld.addEntityToWorld(entityId, player);
    }

    private String nextFallbackName() {
        while (!fallbackNames.isEmpty()) {
            String next = fallbackNames.removeFirst();
            if (next == null || next.trim().isEmpty() || claimedNames.contains(next)) {
                continue;
            }
            claimedNames.add(next);
            return next;
        }
        String generated = "ReplayPlayer" + fallbackCounter++;
        claimedNames.add(generated);
        return generated;
    }
}
