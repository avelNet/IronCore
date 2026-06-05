package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketToggleFlight {
    public PacketToggleFlight() {}

    public PacketToggleFlight(FriendlyByteBuf buffer) {}

    public void toBytes(FriendlyByteBuf buffer) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                // Flight is available for Mk2 and above
                if (suit.getSuitTier().equals("mk2") && suit.getIcingLevel() < 100.0f) {
                    suit.setFlying(!suit.isFlying());
                    
                    // If disabling flight mid-air, ensure Vanilla flying logic is updated immediately
                    if (!suit.isFlying() && !player.isCreative() && !player.isSpectator()) {
                        player.getAbilities().mayfly = false;
                        player.getAbilities().flying = false;
                        player.onUpdateAbilities();
                    }
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
