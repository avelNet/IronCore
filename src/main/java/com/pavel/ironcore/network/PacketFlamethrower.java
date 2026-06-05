package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class PacketFlamethrower {
    public PacketFlamethrower() {}

    public PacketFlamethrower(FriendlyByteBuf buffer) {}

    public void toBytes(FriendlyByteBuf buffer) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (suit.getSuitTier().equals("mk1") && suit.getEnergy() >= 10) {
                    suit.setEnergy(suit.getEnergy() - 10);
                    
                    ServerLevel level = player.serverLevel();
                    Vec3 look = player.getLookAngle();
                    Vec3 pos = player.getEyePosition();

                    // Спавн частиц
                    for (int i = 0; i < 10; i++) {
                        double ox = (player.getRandom().nextDouble() - 0.5) * 0.2;
                        double oy = (player.getRandom().nextDouble() - 0.5) * 0.2;
                        double oz = (player.getRandom().nextDouble() - 0.5) * 0.2;
                        level.sendParticles(ParticleTypes.FLAME, 
                                pos.x + look.x + ox, pos.y + look.y + oy, pos.z + look.z + oz, 
                                1, look.x * 0.5, look.y * 0.5, look.z * 0.5, 0.1);
                    }

                    // Урон сущностям
                    Vec3 targetPos = pos.add(look.scale(3.0));
                    AABB area = new AABB(pos, targetPos).inflate(1.0);
                    List<Entity> entities = level.getEntities(player, area, e -> e instanceof LivingEntity);
                    for (Entity entity : entities) {
                        entity.setSecondsOnFire(3);
                        entity.hurt(player.damageSources().playerAttack(player), 2.0f);
                    }
                }
            });
        });
        return true;
    }
}
