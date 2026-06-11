package com.pavel.ironcore.item;

import com.pavel.ironcore.capability.SuitCapability;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Mk3FrameItem extends BaseSuitItem {
    public Mk3FrameItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public String getTierId() {
        return "mk3";
    }

    @Override
    protected void applyClientPhysics(Player player, SuitCapability suit, ItemStack stack, Level level) {
        if (player.getAbilities().flying) {
            boolean isBoosting = net.minecraft.client.Minecraft.getInstance().options.keySprint.isDown();
            boolean enginesOverheated = suit.getHeat() >= 100.0f;

            if (player.isInWater() && !player.isCreative()) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            } else if (enginesOverheated) {
                Vec3 current = player.getDeltaMovement();
                player.setDeltaMovement(current.x * 0.9, current.y - 0.1, current.z * 0.9);
                player.hasImpulse = true;
            } else if (suit.getEnergy() <= 1000 && suit.getEnergy() >= 4) {
                Vec3 current = player.getDeltaMovement();
                player.setDeltaMovement(current.x * 0.9, current.y - 0.1, current.z * 0.9);
                player.hasImpulse = true;
            } else if (isBoosting && suit.getEnergy() > 1000) {
                Vec3 look = player.getLookAngle();
                Vec3 current = player.getDeltaMovement();
                double maxSpeed = suit.isTurbo() ? 1.25 : 1.0; 
                double currentSpeed = current.length();
                double speedRatio = Math.min(currentSpeed / maxSpeed, 1.0);
                double acceleration = 0.15 + Math.pow(speedRatio, 2.0) * (suit.isTurbo() ? 0.7 : 0.6); 
                Vec3 target = look.scale(maxSpeed);
                double targetY = target.y > 0 ? target.y * 1.2 + 0.1 : target.y * 1.4;
                player.setDeltaMovement(new Vec3(
                    current.x + (target.x - current.x) * acceleration,
                    current.y + (targetY - current.y) * acceleration,
                    current.z + (target.z - current.z) * acceleration
                ));
                player.hasImpulse = true;
            } else if (suit.getEnergy() >= 4) {
                Vec3 current = player.getDeltaMovement();
                double hoverMaxSpeed = 0.3; 
                if (current.length() > hoverMaxSpeed) {
                    player.setDeltaMovement(current.scale(0.85)); 
                    player.hasImpulse = true;
                }
            }
        }
    }

    @Override
    protected boolean applyServerLogic(ServerPlayer player, SuitCapability suit, ItemStack stack, Level level) {
        boolean changed = false;

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        
        boolean systemsFunctional = player.isCreative() || (suit.getIcingLevel() < 100.0f && suit.getHeat() < 100.0f && suit.getEnergy() >= 4 && !player.isInWater());
        player.getAbilities().mayfly = systemsFunctional;
        
        if (player.getAbilities().flying) {
            if (!systemsFunctional) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            } else {
                int energyDrain = suit.isTurbo() ? 80 : 4;
                suit.setEnergy(suit.getEnergy() - energyDrain);
                stack.getOrCreateTag().putInt("SuitEnergy", suit.getEnergy());

                boolean isBoosting = suit.isBoostKeyHeld();
                if (isBoosting && suit.getEnergy() > 1000) {
                    suit.setFlightTimer(suit.getFlightTimer() + 1);
                    suit.setHeat(suit.getHeat() + 0.04f);
                    if (suit.isAutoBoostEnabled() && suit.getFlightTimer() >= 100) {
                        if (!suit.isTurbo()) {
                            suit.setTurbo(true);
                            changed = true;
                        }
                    }
                } else {
                    if (suit.getFlightTimer() > 0 || suit.isTurbo()) {
                        suit.setFlightTimer(0);
                        suit.setTurbo(false);
                        changed = true;
                    }
                }
                
                ServerLevel serverLevel = (ServerLevel) level;
                Vec3 pos = player.position();
                if (suit.isTurbo()) {
                    for (int i = 0; i < 3; i++) {
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.05, 0.05, 0.05, 0.05);
                    }
                    suit.setHeat(suit.getHeat() + 0.08f); 
                } else if (player.tickCount % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
                }
            }
        } else {
            if (suit.getFlightTimer() > 0 || suit.isTurbo()) {
                suit.setFlightTimer(0);
                suit.setTurbo(false);
                changed = true;
            }
        }

        player.onUpdateAbilities();

        if (suit.getIcingLevel() > 0) suit.setIcingLevel(suit.getIcingLevel() - 0.5f);

        float coolingRate = player.isInWater() ? 0.8f : (level.getBiome(player.blockPosition()).value().getBaseTemperature() < 0.2f ? 0.4f : 0.2f);
        if (level.dimension() == Level.NETHER) suit.setHeat(suit.getHeat() + 0.03f);
        if (player.isOnFire() || player.isInLava()) suit.setHeat(suit.getHeat() + 0.5f);
        if (!suit.isBoostKeyHeld() || !player.getAbilities().flying) suit.setHeat(suit.getHeat() - coolingRate);

        player.setTicksFrozen(0);
        return true; 
    }
}
