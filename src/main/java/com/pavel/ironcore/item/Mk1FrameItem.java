package com.pavel.ironcore.item;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketSyncSuitData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

    private boolean isFullSuitEquipped(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.MK1_HELMET.get() &&
               player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.MK1_CHESTPLATE.get() &&
               player.getItemBySlot(EquipmentSlot.LEGS).getItem() == ModItems.MK1_LEGGINGS.get() &&
               player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.MK1_BOOTS.get();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            // Проверяем логику только если этот предмет надет в слот брони
            if (player.getItemBySlot(this.getType().getSlot()) == stack) {
                
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    boolean changed = false;
                    boolean isFull = isFullSuitEquipped(player);

                    if (isFull) {
                        if (!suit.getSuitTier().equals("mk1")) {
                            suit.setSuitTier("mk1");
                            changed = true;
                        }

                        // Каноничные баффы Mk1
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));

                        // Система термозащиты (сопротивление огню за счет энергии)
                        // Максимум 1 минута = 1200 тиков. Если тратить 8 FE в тик, полного реактора (10000) хватит на ~60 секунд.
                        if (player.isOnFire() || player.isInLava()) {
                            if (suit.getEnergy() >= 8) {
                                suit.setEnergy(suit.getEnergy() - 8);
                                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
                                changed = true;
                            }
                        }
                    } else {
                        // Если сет не полный, сбрасываем статус костюма
                        if (suit.getSuitTier().equals("mk1")) {
                            suit.setSuitTier("none");
                            changed = true;
                        }
                    }

                    // Синхронизируем данные каждую секунду (20 тиков) или при изменении
                    if (changed || player.tickCount % 20 == 0) {
                        ModMessages.sendToPlayer(new PacketSyncSuitData(
                                suit.getEnergy(), suit.getMaxEnergy(), suit.getSuitTier(), 
                                suit.getFrameDurability(), suit.getPalladiumPoisoning(),
                                suit.getIcingLevel(), suit.getHeat(), suit.isFlying()), player);
                    }
                });
            }
        }
    }
}
