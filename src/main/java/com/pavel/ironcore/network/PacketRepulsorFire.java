package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketRepulsorFire {
    public PacketRepulsorFire() {}

    public PacketRepulsorFire(FriendlyByteBuf buffer) {}

    public void toBytes(FriendlyByteBuf buffer) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (suit.getSuitTier().equals("mk3") && suit.getEnergy() >= 150) {
                    
                    // Raycast Hitscan Logic
                    double reachDistance = 30.0;
                    Vec3 eyePosition = player.getEyePosition();
                    Vec3 lookVector = player.getLookAngle();
                    Vec3 endPosition = eyePosition.add(lookVector.scale(reachDistance));

                    // Block raycast (to stop beam at walls)
                    HitResult blockHit = player.level().clip(new ClipContext(eyePosition, endPosition, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
                    if (blockHit.getType() != HitResult.Type.MISS) {
                        endPosition = blockHit.getLocation();
                    }

                    // Entity raycast
                    AABB searchBox = player.getBoundingBox().expandTowards(lookVector.scale(reachDistance)).inflate(1.0D);
                    EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(player, eyePosition, endPosition, searchBox, (entity) -> !entity.isSpectator() && entity.isPickable(), reachDistance * reachDistance);

                    if (entityHit != null && entityHit.getEntity() instanceof LivingEntity target) {
                        endPosition = entityHit.getLocation();
                        target.hurt(player.damageSources().playerAttack(player), 10.0f); // 10 урона (5 сердец)
                    }

                    // Spawn Beam Particles
                    ServerLevel serverLevel = (ServerLevel) player.level();
                    double distance = eyePosition.distanceTo(endPosition);
                    for (double i = 0; i < distance; i += 0.5) {
                        Vec3 particlePos = eyePosition.add(lookVector.scale(i));
                        serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, particlePos.x, particlePos.y, particlePos.z, 1, 0.0, 0.0, 0.0, 0.0);
                        serverLevel.sendParticles(ParticleTypes.END_ROD, particlePos.x, particlePos.y, particlePos.z, 1, 0.0, 0.0, 0.0, 0.0);
                    }

                    // Consuming energy (stored in capability, synced later, or directly to chestplate)
                    suit.setEnergy(suit.getEnergy() - 150);
                    
                    net.minecraft.world.item.ItemStack chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                    if(chestplate.getItem() instanceof net.minecraft.world.item.ArmorItem) {
                         chestplate.getOrCreateTag().putInt("SuitEnergy", suit.getEnergy()); 
                    }
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
