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
    private final boolean isMaskOpen;
    private final boolean wasFlyingHorizontally;
    private final int flightTimer;
    private final int entityId;

    public PacketSyncSuitData(int energy, int maxEnergy, String tier, int durability, float poisoning, String reactor, float icingLevel, float heatLevel, boolean isFlying, boolean autoBoostEnabled, boolean isTurbo, boolean hasEmbeddedReactor, boolean isMaskOpen, boolean wasFlyingHorizontally, int flightTimer, int entityId) {
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
        this.isMaskOpen = isMaskOpen;
        this.wasFlyingHorizontally = wasFlyingHorizontally;
        this.flightTimer = flightTimer;
        this.entityId = entityId;
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
        this.isMaskOpen = buffer.readBoolean();
        this.wasFlyingHorizontally = buffer.readBoolean();
        this.flightTimer = buffer.readInt();
        this.entityId = buffer.readInt();
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
        buffer.writeBoolean(isMaskOpen);
        buffer.writeBoolean(wasFlyingHorizontally);
        buffer.writeInt(flightTimer);
        buffer.writeInt(entityId);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
            if (level != null) {
                net.minecraft.world.entity.Entity entity = level.getEntity(entityId);
                if (entity instanceof net.minecraft.world.entity.player.Player player) {
                    player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
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
                        suit.setMaskOpen(isMaskOpen);
                        suit.setWasFlyingHorizontally(wasFlyingHorizontally);
                        suit.setFlightTimer(flightTimer);
                    });
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
