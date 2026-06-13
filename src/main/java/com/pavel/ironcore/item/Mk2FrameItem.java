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
            boolean isBoosting = net.minecraft.client.Minecraft.getInstance().options.keySprint.isDown();
            boolean enginesFrozen = suit.getIcingLevel() >= 100.0f;
            boolean enginesOverheated = suit.getHeat() >= 100.0f;

            if (player.isInWater() && !player.isCreative()) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            } else if (enginesOverheated || enginesFrozen) {
                Vec3 current = player.getDeltaMovement();
                player.setDeltaMovement(current.x * 0.9, current.y - 0.1, current.z * 0.9);
                player.hasImpulse = true;
            } else if (isBoosting && suit.getEnergy() > 1000) {
                Vec3 look = player.getLookAngle();
                Vec3 current = player.getDeltaMovement();
                double maxSpeed = 0.85; 
                double acceleration = 0.15; 
                Vec3 target = look.scale(maxSpeed);
                player.setDeltaMovement(new Vec3(
                    current.x + (target.x - current.x) * acceleration,
                    current.y + (target.y - current.y) * acceleration,
                    current.z + (target.z - current.z) * acceleration
                ));
                player.hasImpulse = true;
            } else if (suit.getEnergy() >= 4) {
                Vec3 current = player.getDeltaMovement();
                double hoverMaxSpeed = 0.278; 
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
        
        boolean systemsFunctional = player.isCreative() || (suit.getIcingLevel() < 100.0f && suit.getHeat() < 100.0f && suit.getEnergy() >= 4 && !player.isInWater());
        player.getAbilities().mayfly = systemsFunctional;
        
        if (player.getAbilities().flying) {
            if (!systemsFunctional) {
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            } else {
                if (player.tickCount % 4 == 0) suit.setEnergy(suit.getEnergy() - 1);
                if (suit.isBoostKeyHeld()) suit.setHeat(suit.getHeat() + 0.04f);
                
                ServerLevel serverLevel = (ServerLevel) level;
                Vec3 pos = player.position();
                if (player.tickCount % 2 == 0) {
                    serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
                }
            }
        }

        // Icing and Heat logic
        if (player.getY() > 170) {
            suit.setIcingLevel(suit.getIcingLevel() + 0.1f);
        } else {
            suit.setIcingLevel(Math.max(0, suit.getIcingLevel() - 0.2f));
        }

        float coolingRate = player.isInWater() ? 0.5f : 0.1f;
        if (!suit.isBoostKeyHeld() || !player.getAbilities().flying) suit.setHeat(Math.max(0, suit.getHeat() - coolingRate));

        player.onUpdateAbilities();
        return true; 
    }
}
