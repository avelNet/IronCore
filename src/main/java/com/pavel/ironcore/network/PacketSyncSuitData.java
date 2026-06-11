package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketSyncSuitData {
    private final int energy;
    private final int maxEnergy;
    private final String tier;
    private final int durability;
    private final float poisoning;
    private final String reactor;
    private final float icingLevel;
    private final float heatLevel;
    private final boolean isFlying;
    private final boolean autoBoostEnabled;
    private final boolean isTurbo;
    private final boolean hasEmbeddedReactor;

    public PacketSyncSuitData(int energy, int maxEnergy, String tier, int durability, float poisoning, String reactor, float icingLevel, float heatLevel, boolean isFlying, boolean autoBoostEnabled, boolean isTurbo, boolean hasEmbeddedReactor) {
        this.energy = energy;
        this.maxEnergy = maxEnergy;
        this.tier = tier;
        this.durability = durability;
        this.poisoning = poisoning;
        this.reactor = reactor;
        this.icingLevel = icingLevel;
        this.heatLevel = heatLevel;
        this.isFlying = isFlying;
        this.autoBoostEnabled = autoBoostEnabled;
        this.isTurbo = isTurbo;
        this.hasEmbeddedReactor = hasEmbeddedReactor;
    }

    public PacketSyncSuitData(FriendlyByteBuf buffer) {
        this.energy = buffer.readInt();
        this.maxEnergy = buffer.readInt();
        this.tier = buffer.readUtf();
        this.durability = buffer.readInt();
        this.poisoning = buffer.readFloat();
        this.reactor = buffer.readUtf();
        this.icingLevel = buffer.readFloat();
        this.heatLevel = buffer.readFloat();
        this.isFlying = buffer.readBoolean();
        this.autoBoostEnabled = buffer.readBoolean();
        this.isTurbo = buffer.readBoolean();
        this.hasEmbeddedReactor = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeInt(energy);
        buffer.writeInt(maxEnergy);
        buffer.writeUtf(tier);
        buffer.writeInt(durability);
        buffer.writeFloat(poisoning);
        buffer.writeUtf(reactor);
        buffer.writeFloat(icingLevel);
        buffer.writeFloat(heatLevel);
        buffer.writeBoolean(isFlying);
        buffer.writeBoolean(autoBoostEnabled);
        buffer.writeBoolean(isTurbo);
        buffer.writeBoolean(hasEmbeddedReactor);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            net.minecraft.client.Minecraft.getInstance().player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                suit.setMaxEnergy(maxEnergy);
                suit.setEnergy(energy);
                suit.setSuitTier(tier);
                suit.setFrameDurability(durability);
                suit.setPalladiumPoisoning(poisoning);
                suit.setActiveReactorType(reactor);
                suit.setIcingLevel(icingLevel);
                suit.setHeat(heatLevel);
                suit.setFlying(isFlying);
                suit.setAutoBoostEnabled(autoBoostEnabled);
                suit.setTurbo(isTurbo);
                suit.setEmbeddedReactor(hasEmbeddedReactor);
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
