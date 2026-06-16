package com.pavel.ironcore.item;

import com.pavel.ironcore.capability.SuitCapability;
import com.pavel.ironcore.flight.FlightConfig;
import com.pavel.ironcore.flight.FlightPhysics;
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

public class Mk2FrameItem extends BaseSuitItem {
    public Mk2FrameItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public String getTierId() {
        return "mk2";
    }

    @Override
    protected void applyClientPhysics(Player player, SuitCapability suit, ItemStack stack, Level level) {
        if (player.getAbilities().flying) {
            boolean isBoosting = net.minecraft.client.Minecraft.getInstance().options.keySprint.isDown() && !suit.isMaskOpen();
            boolean enginesFrozen = suit.getIcingLevel() >= 100.0f;
            boolean enginesOverheated = suit.getHeat() >= 100.0f;

            if (player.isInWater() && !player.isCreative()) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            } else if (enginesOverheated || enginesFrozen) {
                player.setDeltaMovement(FlightPhysics.computeOverheatVelocity(player.getDeltaMovement()));
                player.hasImpulse = true;
            } else if (isBoosting && suit.getEnergy() > 1000) {
                Vec3 look = player.getLookAngle();
                player.setDeltaMovement(FlightPhysics.computeBoostVelocity(player.getDeltaMovement(), look, FlightConfig.MK2, false));
                player.hasImpulse = true;
            } else if (suit.getEnergy() >= 4) {
                Vec3 newVelocity = FlightPhysics.computeHoverVelocity(player.getDeltaMovement(), FlightConfig.MK2);
                if (newVelocity != player.getDeltaMovement()) {
                    player.setDeltaMovement(newVelocity);
                    player.hasImpulse = true;
                }
            }

            Vec3 landed = FlightPhysics.applyAutoLandOverride(player.getDeltaMovement(), suit.isAutoLandEnabled(), player.onGround());
            if (landed != player.getDeltaMovement()) {
                player.setDeltaMovement(landed);
                player.hasImpulse = true;
            }
        }
    }

    @Override
    protected boolean applyServerLogic(ServerPlayer player, SuitCapability suit, ItemStack stack, Level level) {
        boolean changed = false;
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        
        boolean systemsFunctional = player.isCreative() || (suit.getIcingLevel() < 100.0f && suit.getHeat() < 100.0f && suit.getEnergy() >= 4 && !player.isInWater() && suit.getWaterExitCooldown() <= 0);
        boolean prevMayfly = player.getAbilities().mayfly;
        player.getAbilities().mayfly = systemsFunctional;

        // Track water exit cooldown
        if (player.isInWater()) {
            suit.setWaterExitCooldown(20);
        } else if (suit.getWaterExitCooldown() > 0) {
            suit.setWaterExitCooldown(suit.getWaterExitCooldown() - 1);
        }
        if (suit.getLaunchCooldown() > 0) suit.setLaunchCooldown(suit.getLaunchCooldown() - 1);

        if (player.getAbilities().flying) {
            if (!systemsFunctional) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            } else {
                if (player.tickCount % 4 == 0) suit.setEnergy(suit.getEnergy() - 1);
                if (suit.isBoostKeyHeld() && !suit.isMaskOpen()) suit.setHeat(suit.getHeat() + 0.04f);
                
                ServerLevel serverLevel = (ServerLevel) level;
                Vec3 pos = player.position();
                if (player.tickCount % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
                }
            }
        }

        // Icing and Heat logic
        if (player.getY() > 170 && player.getAbilities().flying) {
            suit.setIcingLevel(suit.getIcingLevel() + 0.1f);
        } else {
            suit.setIcingLevel(Math.max(0, suit.getIcingLevel() - 0.2f));
        }

        float coolingRate = player.isInWater() ? 0.5f : 0.1f;
        if (!suit.isBoostKeyHeld() || !player.getAbilities().flying || suit.isMaskOpen()) suit.setHeat(Math.max(0, suit.getHeat() - coolingRate));

        if (prevMayfly != player.getAbilities().mayfly) {
            player.onUpdateAbilities();
        }
        return true; 
    }
}
