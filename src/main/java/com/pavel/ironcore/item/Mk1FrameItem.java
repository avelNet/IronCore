package com.pavel.ironcore.item;

import com.pavel.ironcore.capability.SuitCapability;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Mk1FrameItem extends BaseSuitItem {
    public Mk1FrameItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public String getTierId() {
        return "mk1";
    }

    @Override
    protected void applyClientPhysics(Player player, SuitCapability suit, ItemStack stack, Level level) {
        if (player.getAbilities().flying && !player.onGround() && !player.isInWater() && !player.isInLava()) {
            if (player.isSprinting()) {
                Vec3 look = player.getLookAngle();
                Vec3 current = player.getDeltaMovement();
                double speed = 0.55; 
                player.setDeltaMovement(new Vec3(
                    current.x + (look.x * speed - current.x) * 0.1,
                    current.y + (look.y * speed - current.y) * 0.1,
                    current.z + (look.z * speed - current.z) * 0.1
                ));
                player.hasImpulse = true;
            }
        }
    }

    @Override
    protected boolean applyServerLogic(ServerPlayer player, SuitCapability suit, ItemStack stack, Level level) {
        if (suit.getEnergy() > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }

        if (player.isOnFire() || player.isInLava()) {
            if (suit.getEnergy() >= 8) {
                suit.setEnergy(suit.getEnergy() - 8);
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
                return true;
            }
        }
        return false;
    }
}
