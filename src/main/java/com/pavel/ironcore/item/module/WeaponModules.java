package com.pavel.ironcore.item.module;

import com.pavel.ironcore.capability.SuitCapability;
import com.pavel.ironcore.entity.MicroMissileEntity;
import com.pavel.ironcore.network.ModMessages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

/** Боевые модули (Weaponry): G — микроракета, Shift+G — юнибим. */
public final class WeaponModules {
    private static final int MISSILE_COST = 1500;
    private static final int MISSILE_COOLDOWN = 30;
    private static final double MISSILE_LOCK_RANGE = 32.0;
    private static final double MISSILE_LOCK_CONE = 0.9; // cos ≈ 25°

    private static final int UNIBEAM_COST = 6000;
    private static final int UNIBEAM_COOLDOWN = 200;
    private static final float UNIBEAM_HEAT = 25.0f;
    private static final float UNIBEAM_DAMAGE = 24.0f;
    private static final double UNIBEAM_RANGE = 40.0;
    private static final double UNIBEAM_RADIUS = 0.8;

    private WeaponModules() { }

    /** Серверный тик: перезарядка. */
    public static void tickCooldowns(SuitCapability suit) {
        if (suit.getMissileCooldown() > 0) suit.setMissileCooldown(suit.getMissileCooldown() - 1);
        if (suit.getUnibeamCooldown() > 0) suit.setUnibeamCooldown(suit.getUnibeamCooldown() - 1);
    }

    public static void fire(ServerPlayer player, SuitCapability suit, boolean secondary) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (suit.getSuitTier().equals("none")) return;
        boolean hasMissiles = SuitModules.has(chest, SuitModule.MICRO_MISSILES);
        boolean hasUnibeam = SuitModules.has(chest, SuitModule.UNIBEAM);
        if (!hasMissiles && !hasUnibeam) {
            status(player, "weapon.ironcore.none");
            return;
        }
        if (suit.getHeat() >= 100.0f) {
            status(player, "weapon.ironcore.overheated");
            return;
        }
        // Shift выбирает юнибим; если стоит только один модуль — стреляет он
        if (hasUnibeam && (secondary || !hasMissiles)) {
            fireUnibeam(player, suit);
        } else {
            fireMissile(player, suit);
        }
    }

    private static void fireMissile(ServerPlayer player, SuitCapability suit) {
        if (suit.getMissileCooldown() > 0) return;
        if (suit.getEnergy() < MISSILE_COST) {
            status(player, "weapon.ironcore.no_energy");
            return;
        }
        Vec3 look = player.getLookAngle();
        Vec3 shoulder = Vec3.directionFromRotation(0, player.getYRot() + (player.tickCount % 2 == 0 ? 90 : -90)).scale(0.45);
        Vec3 from = player.getEyePosition().add(shoulder).add(0, -0.1, 0).add(look.scale(0.6));
        LivingEntity target = findTarget(player, look);

        player.level().addFreshEntity(new MicroMissileEntity(player.level(), player, from, look, target));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 0.8f);

        suit.setEnergy(suit.getEnergy() - MISSILE_COST);
        suit.setMissileCooldown(MISSILE_COOLDOWN);
        if (target != null) {
            player.displayClientMessage(Component.translatable("weapon.ironcore.locked", target.getDisplayName()), true);
        }
        ModMessages.sendSyncPacket(player);
    }

    /** Ближайшее живое существо в конусе взгляда с прямой видимостью. */
    @Nullable
    private static LivingEntity findTarget(ServerPlayer player, Vec3 look) {
        Vec3 eye = player.getEyePosition();
        List<LivingEntity> candidates = player.level().getEntitiesOfClass(LivingEntity.class,
                player.getBoundingBox().inflate(MISSILE_LOCK_RANGE),
                e -> e != player && e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator())));
        return candidates.stream()
                .filter(e -> {
                    Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                    return to.length() <= MISSILE_LOCK_RANGE && to.normalize().dot(look) >= MISSILE_LOCK_CONE
                            && player.hasLineOfSight(e);
                })
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);
    }

    private static void fireUnibeam(ServerPlayer player, SuitCapability suit) {
        if (suit.getUnibeamCooldown() > 0) {
            player.displayClientMessage(Component.translatable("weapon.ironcore.unibeam_cooldown",
                    (suit.getUnibeamCooldown() + 19) / 20), true);
            return;
        }
        if (suit.getEnergy() < UNIBEAM_COST) {
            status(player, "weapon.ironcore.no_energy");
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        // Луч из реактора на груди
        Vec3 chest = player.position().add(0, player.getBbHeight() * 0.62, 0).add(look.scale(0.4));
        Vec3 end = chest.add(look.scale(UNIBEAM_RANGE));
        HitResult blockHit = level.clip(new ClipContext(chest, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) end = blockHit.getLocation();

        // Пробивает всех на линии
        AABB area = new AABB(chest, end).inflate(UNIBEAM_RADIUS);
        Vec3 beam = end.subtract(chest);
        double length = beam.length();
        Vec3 dir = beam.normalize();
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, area, e -> e != player && e.isAlive())) {
            Vec3 center = victim.getBoundingBox().getCenter();
            double along = center.subtract(chest).dot(dir);
            if (along < 0 || along > length) continue;
            double dist = center.subtract(chest.add(dir.scale(along))).length();
            if (dist <= UNIBEAM_RADIUS + victim.getBbWidth() / 2) {
                victim.hurt(player.damageSources().indirectMagic(player, player), UNIBEAM_DAMAGE);
                victim.setSecondsOnFire(4);
                victim.knockback(1.2, -dir.x, -dir.z);
            }
        }

        for (double d = 0; d < length; d += 0.4) {
            Vec3 p = chest.add(dir.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 2, 0.08, 0.08, 0.08, 0.0);
            if (((int) (d * 2.5)) % 3 == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.15, 0.15, 0.15, 0.02);
            }
        }
        level.sendParticles(ParticleTypes.FLASH, chest.x, chest.y, chest.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0, 0, 0, 0);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.3f);

        suit.setEnergy(suit.getEnergy() - UNIBEAM_COST);
        suit.setHeat(suit.getHeat() + UNIBEAM_HEAT);
        suit.setUnibeamCooldown(UNIBEAM_COOLDOWN);
        ModMessages.sendSyncPacket(player);
    }

    private static void status(ServerPlayer player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }
}
