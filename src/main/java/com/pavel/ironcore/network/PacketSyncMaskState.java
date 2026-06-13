package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncMaskState {
    private final boolean isOpen;

    public PacketSyncMaskState(boolean isOpen) {
        this.isOpen = isOpen;
    }

    public PacketSyncMaskState(FriendlyByteBuf buf) {
        this.isOpen = buf.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(isOpen);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            net.minecraft.server.level.ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    suit.setMaskOpen(isOpen);
                    ModMessages.sendSyncPacket(player); // КРИТИЧЕСКИ ВАЖНО: Синхронизируем обратно на клиент!
                });
            }
        });
        return true;
    }
}
