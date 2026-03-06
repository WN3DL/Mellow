package com.roxiun.mellow.feature.replay;

import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;

public class ReplayNetworkManager extends NetworkManager {

    public ReplayNetworkManager() {
        super(EnumPacketDirection.CLIENTBOUND);
    }

    @Override
    public void sendPacket(Packet packetIn) {
        // Offline replay should never send packets.
    }

    @Override
    public void closeChannel(IChatComponent message) {
        super.closeChannel(message == null ? new ChatComponentText("Replay closed") : message);
    }
}
