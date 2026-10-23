package com.pavel.ironcore.entity;

import com.pavel.ironcore.item.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Микроракета модуля «Микроракеты»: летит с постоянной скоростью, доворачивает к цели,
 * взрывается при попадании или через 4 с. Взрыв ранит существ, но не ломает блоки.
 */
public class MicroMissileEntity extends AbstractHurtingProjectile implements ItemSupplier {
    private static final double SPEED = 1.4;
    private static final double TURN_RATE = 0.22;
    private static final int LIFETIME = 80;
    private static final float DIRECT_DAMAGE = 8.0f;
    private static final float EXPLOSION_POWER = 1.8f;

    @Nullable
    private LivingEntity target;

    public MicroMissileEntity(EntityType<? extends MicroMissileEntity> type, Level level) {
        super(type, level);
    }

    public MicroMissileEntity(Level level, LivingEntity shooter, Vec3 from, Vec3 direction, @Nullable LivingEntity target) {
        super(ModEntities.MICRO_MISSILE.get(), level);
        setOwner(shooter);
        setPos(from);
        setDeltaMovement(direction.normalize().scale(SPEED));
        // Без собственного ускорения AbstractHurtingProjectile: скорость задаём сами в tick()
        xPower = 0;
        yPower = 0;
        zPower = 0;
        this.target = target;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (tickCount > LIFETIME) {
                detonate();
                return;
            }
            if (target != null && target.isAlive()) {
                Vec3 toTarget = target.getBoundingBox().getCenter().subtract(position()).normalize();
                Vec3 heading = getDeltaMovement().normalize();
                setDeltaMovement(heading.add(toTarget.subtract(heading).scale(TURN_RATE)).normalize().scale(SPEED));
                hasImpulse = true;
            }
        }
        super.tick();
    }

    @Override
    protected float getInertia() {
        return 1.0f;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SMOKE;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!level().isClientSide) {
            Entity owner = getOwner();
            result.getEntity().hurt(damageSources().explosion(this, owner), DIRECT_DAMAGE);
        }
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) detonate();
    }

    private void detonate() {
        level().explode(this, getX(), getY(), getZ(), EXPLOSION_POWER, false, Level.ExplosionInteraction.NONE);
        discard();
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ModItems.MICRO_MISSILE.get());
    }
}
