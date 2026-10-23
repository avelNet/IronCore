package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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
                if ((suit.getSuitTier().equals("mk1") || suit.getSuitTier().equals("mk2")) && suit.getEnergy() >= 10) {
                    suit.setEnergy(suit.getEnergy() - 10);
                    
                    ServerLevel level = player.serverLevel();
                    Vec3 look = player.getLookAngle();
                    Vec3 pos = player.getEyePosition();

                    spawnFlameParticles(level, player, look);

                    // Урон и поджог сущностям в конусе перед игроком
                    Vec3 targetPos = pos.add(look.scale(4.0));
                    AABB area = new AABB(pos, targetPos).inflate(1.5);
                    List<Entity> entities = level.getEntities(player, area, e -> e instanceof LivingEntity);
                    for (Entity entity : entities) {
                        entity.setSecondsOnFire(5);
                        entity.hurt(player.damageSources().inFire(), 3.0f);
                    }
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }

    /**
     * Струя пламени из правой руки. count=0 в sendParticles означает «одна частица с заданным
     * направлением»: (dx, dy, dz) — направление, speed — скорость, так что пламя летит вперёд.
     */
    private static void spawnFlameParticles(ServerLevel level, ServerPlayer player, Vec3 look) {
        Vec3 right = Vec3.directionFromRotation(0, player.getYRot() + 90.0f);
        Vec3 hand = player.getEyePosition().add(right.scale(0.35)).add(0, -0.45, 0).add(look.scale(0.5));
        var random = level.getRandom();
        for (int i = 0; i < 24; i++) {
            double spread = 0.09;
            Vec3 dir = look.add(random.nextGaussian() * spread, random.nextGaussian() * spread, random.nextGaussian() * spread).normalize();
            double speed = 0.35 + random.nextDouble() * 0.25;
            level.sendParticles(i % 4 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME,
                    hand.x, hand.y, hand.z, 0, dir.x, dir.y, dir.z, speed);
        }
        if (random.nextInt(3) == 0) {
            Vec3 tip = hand.add(look.scale(3.5));
            level.sendParticles(ParticleTypes.LARGE_SMOKE, tip.x, tip.y, tip.z, 2, 0.3, 0.2, 0.3, 0.01);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.4f, 1.4f + random.nextFloat() * 0.3f);
    }
}
