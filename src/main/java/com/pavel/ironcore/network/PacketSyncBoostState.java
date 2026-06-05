package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncBoostState {
    private final boolean isBoosting;

    public PacketSyncBoostState(boolean isBoosting) { this.isBoosting = isBoosting; }

    public PacketSyncBoostState(FriendlyByteBuf buffer) { this.isBoosting = buffer.readBoolean(); }

    public void toBytes(FriendlyByteBuf buffer) { buffer.writeBoolean(isBoosting); }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    suit.setBoostKeyHeld(isBoosting);
                });
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
