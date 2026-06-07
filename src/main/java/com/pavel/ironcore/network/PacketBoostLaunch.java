package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
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
                if ((suit.getSuitTier().equals("mk2") || suit.getSuitTier().equals("mk3")) && suit.getEnergy() >= 1000 && suit.getIcingLevel() < 100.0f) {
                    suit.setFlying(true);
                    player.getAbilities().mayfly = true;
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    
                    Vec3 look = player.getLookAngle();
                    // Launch straight up and slightly forward
                    double yBoost = suit.getSuitTier().equals("mk3") ? 2.0 : 1.5; // Mk3 прыгает выше
                    Vec3 boost = new Vec3(look.x * 0.5, yBoost, look.z * 0.5);
                    player.setDeltaMovement(boost);
                    player.hasImpulse = true;
                    player.hurtMarked = true;
                    suit.setEnergy(suit.getEnergy() - 100); // 100 FE cost for instant launch
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
