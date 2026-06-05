package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncSuitData {
    private final int energy;
    private final String tier;
    private final int durability;
    private final int poisoning;

    public PacketSyncSuitData(int energy, String tier, int durability, int poisoning) {
        this.energy = energy;
        this.tier = tier;
        this.durability = durability;
        this.poisoning = poisoning;
    }

    public PacketSyncSuitData(FriendlyByteBuf buffer) {
        this.energy = buffer.readInt();
        this.tier = buffer.readUtf();
        this.durability = buffer.readInt();
        this.poisoning = buffer.readInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeInt(energy);
        buffer.writeUtf(tier);
        buffer.writeInt(durability);
        buffer.writeInt(poisoning);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Client side
            net.minecraft.client.Minecraft.getInstance().player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                suit.setEnergy(energy);
                suit.setSuitTier(tier);
                suit.setFrameDurability(durability);
                suit.setPalladiumPoisoning(poisoning);
            });
        });
        return true;
    }
}
