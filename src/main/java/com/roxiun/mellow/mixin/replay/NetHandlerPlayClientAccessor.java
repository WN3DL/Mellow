package com.roxiun.mellow.mixin.replay;

import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NetHandlerPlayClient.class)
public interface NetHandlerPlayClientAccessor {
    @Accessor("clientWorldController")
    void mellow$setClientWorldController(WorldClient worldClient);

    @Accessor("doneLoadingTerrain")
    void mellow$setDoneLoadingTerrain(boolean value);
}
