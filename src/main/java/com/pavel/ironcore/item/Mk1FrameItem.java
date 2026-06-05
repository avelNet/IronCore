package com.pavel.ironcore.item;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketSyncSuitData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class Mk1FrameItem extends ArmorItem {
    public Mk1FrameItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chest == stack) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    boolean changed = false;
                    if (!suit.getSuitTier().equals("mk1")) {
                        suit.setSuitTier("mk1");
                        changed = true;
                    }
                    
                    // В альфе: если надет костюм, он потихоньку тратит энергию (тест HUD)
                    if (suit.getEnergy() > 0) {
                        suit.setEnergy(suit.getEnergy() - 1);
                        changed = true;
                    }

                    if (changed || player.tickCount % 20 == 0) {
                        ModMessages.sendToPlayer(new PacketSyncSuitData(
                                suit.getEnergy(), suit.getSuitTier(), 
                                suit.getFrameDurability(), suit.getPalladiumPoisoning()), player);
                    }
                });
            }
        }
    }
}
