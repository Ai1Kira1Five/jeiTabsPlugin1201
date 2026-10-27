package com.aikirafive.jeiendlesstabs.network;

import com.aikirafive.jeiendlesstabs.container.ContainerAddonState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class PacketChangeTab {
    private final int tabIndex;

    public PacketChangeTab(int tabIndex) {
        this.tabIndex = tabIndex;
    }

    public PacketChangeTab(FriendlyByteBuf buf) {
        this.tabIndex = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.tabIndex);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.containerMenu instanceof ContainerAddonState state) {
                state.jeiAddon$setCurrentTab(this.tabIndex);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}