package com.roxiun.mellow.mixin.replay;

import com.roxiun.mellow.feature.replay.ReplayManager;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetworkManager.class)
public class NetworkManagerReplayCaptureMixin {

    @Inject(method = "channelRead0", at = @At("HEAD"))
    private void mellow$captureInboundPacket(
        ChannelHandlerContext context,
        Packet<?> packet,
        CallbackInfo ci
    ) {
        ReplayManager.getInstance().onInboundPacket(packet);
    }
}
