package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncAutoBoost {
    private final boolean enabled;

    public PacketSyncAutoBoost(boolean enabled) {
        this.enabled = enabled;
    }

    public PacketSyncAutoBoost(FriendlyByteBuf buffer) {
        this.enabled = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(enabled);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    suit.setAutoBoostEnabled(enabled);

                    // Sync back to all clients
                    ModMessages.sendSyncPacket(player);
                    
                    String status = enabled ? "§aENABLED" : "§cDISABLED";
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§6TURBO MODE: " + status), true);
                });
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
