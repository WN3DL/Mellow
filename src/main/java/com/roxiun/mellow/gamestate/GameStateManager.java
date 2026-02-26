package com.roxiun.mellow.gamestate;

import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.hypixel.data.type.GameType;
import net.hypixel.data.type.ServerType;
import net.hypixel.modapi.HypixelModAPI;
import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import net.hypixel.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import net.hypixel.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;

public class GameStateManager {

    private final AtomicReference<GameSnapshot> snapshot =
        new AtomicReference<>(GameSnapshot.empty());
    private final CopyOnWriteArrayList<Consumer<GameSnapshot>> listeners =
        new CopyOnWriteArrayList<>();

    private boolean initialized;
    private int tickCounter;
    private long lastPartyRequestMillis;

    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        HypixelModAPI api = HypixelModAPI.getInstance();
        api.registerHandler(ClientboundLocationPacket.class, this::handleLocationPacket);
        api.registerHandler(ClientboundPartyInfoPacket.class, this::handlePartyInfoPacket);
        api.subscribeToEventPacket(ClientboundLocationPacket.class);

        initialized = true;
    }

    public GameSnapshot getSnapshot() {
        return snapshot.get();
    }

    public void addListener(Consumer<GameSnapshot> listener) {
        listeners.add(listener);
    }

    public void onClientTick() {
        initialize();

        tickCounter++;
        if (tickCounter % 20 != 0) {
            return;
        }

        boolean onHypixel = HypixelUtils.INSTANCE.isHypixel();
        GameSnapshot current = snapshot.get();

        if (!onHypixel) {
            if (current.isOnHypixel()) {
                publish(GameSnapshot.empty());
            }
            return;
        }

        if (!current.isOnHypixel()) {
            publish(current.withConnection(true));
            current = snapshot.get();
        }

        if (current.getGameType() == GameType.BEDWARS && !current.isLobby()) {
            boolean pregame = detectBedwarsPregame();
            if (pregame != current.isPregame()) {
                publish(current.withPregame(pregame));
            }
        }

        requestPartyInfo(false);
    }

    public void onWorldChange() {
        publish(GameSnapshot.empty());
    }

    private void handleLocationPacket(ClientboundLocationPacket packet) {
        GameSnapshot current = snapshot.get();

        GameType gameType = null;
        if (packet.getServerType().isPresent()) {
            ServerType serverType = packet.getServerType().get();
            if (serverType instanceof GameType) {
                gameType = (GameType) serverType;
            }
        }

        boolean lobby = packet.getLobbyName().isPresent();
        boolean pregame = gameType == GameType.BEDWARS &&
        !lobby &&
        detectBedwarsPregame();

        GameSnapshot next = new GameSnapshot(
            true,
            packet.getServerName(),
            gameType,
            packet.getMode().orElse(""),
            packet.getMap().orElse(""),
            lobby,
            pregame,
            current.getPartyState(),
            System.currentTimeMillis()
        );

        publish(next);
        requestPartyInfo(true);
    }

    private void handlePartyInfoPacket(ClientboundPartyInfoPacket packet) {
        PartyState partyState;

        if (!packet.isInParty()) {
            partyState = PartyState.empty();
        } else {
            Map<UUID, PartyState.PartyRole> members = new HashMap<>();
            UUID leader = null;

            for (Map.Entry<UUID, ClientboundPartyInfoPacket.PartyMember> entry : packet
                .getMemberMap()
                .entrySet()) {
                PartyState.PartyRole role = toRole(entry.getValue().getRole());
                if (role == PartyState.PartyRole.LEADER) {
                    leader = entry.getKey();
                }
                members.put(entry.getKey(), role);
            }

            partyState = new PartyState(true, leader, members);
        }

        publish(snapshot.get().withPartyState(partyState));
    }

    private PartyState.PartyRole toRole(
        ClientboundPartyInfoPacket.PartyRole role
    ) {
        switch (role) {
            case LEADER:
                return PartyState.PartyRole.LEADER;
            case MOD:
                return PartyState.PartyRole.MOD;
            case MEMBER:
            default:
                return PartyState.PartyRole.MEMBER;
        }
    }

    private void requestPartyInfo(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastPartyRequestMillis < 5000) {
            return;
        }

        try {
            HypixelModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket());
            lastPartyRequestMillis = now;
        } catch (Exception ignored) {}
    }

    private boolean detectBedwarsPregame() {
        List<String> lines = com.roxiun.mellow.util.scoreboard.ScoreboardUtils.getSidebarLines();
        if (lines.isEmpty()) {
            return false;
        }

        for (String line : lines) {
            String normalized = line.toLowerCase(Locale.ROOT).trim();
            if (normalized.startsWith("players:")) {
                return true;
            }
        }

        return false;
    }

    private void publish(GameSnapshot next) {
        snapshot.set(next);
        for (Consumer<GameSnapshot> listener : listeners) {
            try {
                listener.accept(next);
            } catch (Exception ignored) {}
        }
    }
}
