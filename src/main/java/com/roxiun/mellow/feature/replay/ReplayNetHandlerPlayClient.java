package com.roxiun.mellow.feature.replay;

import com.mojang.authlib.GameProfile;
import com.roxiun.mellow.mixin.replay.NetHandlerPlayClientAccessor;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S06PacketUpdateHealth;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S1FPacketSetExperience;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S30PacketWindowItems;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S37PacketStatistics;
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
}
