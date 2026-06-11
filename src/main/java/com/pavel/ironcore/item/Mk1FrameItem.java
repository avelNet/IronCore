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

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        if (slot == EquipmentSlot.LEGS) {
            return "ironcore:textures/models/armor/mk1_layer_2.png";
        }
        return "ironcore:textures/models/armor/mk1_layer_1.png";
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
            if (this.getType() == ArmorItem.Type.CHESTPLATE && player.getItemBySlot(EquipmentSlot.CHEST) == stack) {
                
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    boolean changed = false;
                    boolean isFull = isFullSuitEquipped(player);

                    if (isFull) {
                        // Читаем данные реактора из НАГРУДНИКА
                        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
                        net.minecraft.nbt.CompoundTag chestTag = chestplate.getOrCreateTag();
                        
                        String installedReactor = chestTag.getString("InstalledReactor");
                        int suitEnergy = chestTag.getInt("SuitEnergy");
                        int suitMaxEnergy = chestTag.getInt("SuitMaxEnergy");

                        // Если реактора нет - выключаем системы
                        if ((installedReactor.isEmpty() || installedReactor.equals("none")) && !suit.hasEmbeddedReactor()) {
                            if (!suit.getSuitTier().equals("none")) {
                                suit.setSuitTier("none");
                                suit.setActiveReactorType("none");
                                suit.setEnergy(0);
                                suit.setMaxEnergy(0);
                                ModMessages.sendToPlayer(new PacketSyncSuitData(
                                        suit.getEnergy(), suit.getMaxEnergy(), suit.getSuitTier(), 
                                        suit.getFrameDurability(), suit.getPalladiumPoisoning(), suit.getActiveReactorType(),
                                        suit.getIcingLevel(), suit.getHeat(), suit.isFlying(),
                                        suit.isAutoBoostEnabled(), suit.isTurbo(), suit.hasEmbeddedReactor()), player);
                            }
                            return; 
                        }

                        if (!suit.getSuitTier().equals("mk1")) {
                            suit.setSuitTier("mk1");
                            changed = true;
                        }

                        if (suit.hasEmbeddedReactor()) {
                            suit.setActiveReactorType("palladium");
                        } else {
                            // Синхронизируем реактор
                            if (installedReactor.contains("palladium")) suit.setActiveReactorType("palladium");
                            else if (installedReactor.contains("coal")) suit.setActiveReactorType("coal");
                            
                            suit.setMaxEnergy(suitMaxEnergy);
                            if (suit.getEnergy() != suitEnergy) {
                               suit.setEnergy(suitEnergy); 
                            }
                        }

                        // Каноничные баффы Mk1 (если есть энергия)
                        if (suit.getEnergy() > 0) {
                            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
                        }

                        // Система термозащиты
                        if (player.isOnFire() || player.isInLava()) {
                            if (suit.getEnergy() >= 8) {
                                suit.setEnergy(suit.getEnergy() - 8);
                                if (!suit.hasEmbeddedReactor()) {
                                    chestTag.putInt("SuitEnergy", suit.getEnergy()); // СОХРАНЯЕМ В НАГРУДНИК
                                }
                                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
                                changed = true;
                            }
                        }
                    } else {
                        if (suit.getSuitTier().equals("mk1")) {
                            suit.setSuitTier("none");
                            changed = true;
                        }
                    }

                    if (changed || player.tickCount % 20 == 0) {
                        ModMessages.sendToPlayer(new PacketSyncSuitData(
                                suit.getEnergy(), suit.getMaxEnergy(), suit.getSuitTier(), 
                                suit.getFrameDurability(), suit.getPalladiumPoisoning(), suit.getActiveReactorType(),
                                suit.getIcingLevel(), suit.getHeat(), suit.isFlying(),
                                suit.isAutoBoostEnabled(), suit.isTurbo(), suit.hasEmbeddedReactor()), player);
                    }
                });
            }
        }
    }
}
