package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.flight.FlightConfig;
import com.pavel.ironcore.flight.FlightPhysics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketBoostLaunch {
    public PacketBoostLaunch() {}

    public PacketBoostLaunch(FriendlyByteBuf buffer) {}

    public void toBytes(FriendlyByteBuf buffer) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                boolean tierEligible = suit.getSuitTier().equals("mk2") || suit.getSuitTier().equals("mk3");
                if (tierEligible && suit.getEnergy() >= 1000 && suit.getIcingLevel() < 100.0f
                        && !suit.isMaskOpen() && suit.getLaunchCooldown() <= 0
                        && (!player.isInWater() || player.isCreative())) {
                    suit.setFlying(true);
                    player.getAbilities().mayfly = true;
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();

                    FlightConfig config = suit.getSuitTier().equals("mk3") ? FlightConfig.MK3 : FlightConfig.MK2;
                    Vec3 boost = FlightPhysics.computeLaunchVelocity(player.getLookAngle(), config);

                    player.setDeltaMovement(boost);
                    player.hasImpulse = true;
                    player.hurtMarked = true;
                    suit.setEnergy(suit.getEnergy() - 100); // 100 FE cost for instant launch
                    suit.setLaunchCooldown(10); // anti-spam, server-authoritative
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
