package com.pavel.ironcore.item;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketSyncSuitData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Mk2FrameItem extends ArmorItem {
    public Mk2FrameItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    private boolean isFullSuitEquipped(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.MK2_HELMET.get() &&
               player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.MK2_CHESTPLATE.get() &&
               player.getItemBySlot(EquipmentSlot.LEGS).getItem() == ModItems.MK2_LEGGINGS.get() &&
               player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.MK2_BOOTS.get();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            if (player.getItemBySlot(this.getType().getSlot()) == stack) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    boolean changed = false;
                    boolean isFull = isFullSuitEquipped(player);

                    if (isFull) {
                        if (!suit.getSuitTier().equals("mk2")) {
                            suit.setSuitTier("mk2");
                            suit.setMaxEnergy(50000); // Mk2 has larger capacity
                            changed = true;
                        }

                        // Flight mechanics
                        if (suit.isFlying()) {
                            // Reduced consumption: 4 FE per tick (80 FE/s). 50k FE = ~10.4 minutes of continuous flight.
                            if (suit.getEnergy() >= 4 && suit.getIcingLevel() < 100.0f) {
                                suit.setEnergy(suit.getEnergy() - 4); 
                                player.getAbilities().mayfly = true;
                                player.getAbilities().flying = true;
                                player.onUpdateAbilities();

                                // Server-side particles for thrusters (visual feedback for multiplayer)
                                ServerLevel serverLevel = (ServerLevel) level;
                                Vec3 pos = player.position();
                                if (player.tickCount % 2 == 0) {
                                    serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
                                }
                            } else {
                                // Out of energy or system failure due to icing
                                suit.setFlying(false);
                                player.getAbilities().mayfly = false;
                                player.getAbilities().flying = false;
                                player.onUpdateAbilities();
                                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cSYSTEM FAILURE: Flight disabled!"), true);
                                changed = true;
                            }
                        } else {
                            if (player.getAbilities().mayfly && !player.isCreative() && !player.isSpectator()) {
                                player.getAbilities().mayfly = false;
                                player.getAbilities().flying = false;
                                player.onUpdateAbilities();
                            }
                        }

                        // Icing mechanics
                        double yPos = player.getY();
                        if (yPos > 200) { // Обледенение начинается с высоты 200
                            // Calculate icing rate based on altitude
                            float icingRate = (float) ((yPos - 200) * 0.05f); 
                            
                            // Check biomes for extra cold
                            float biomeTemp = level.getBiome(player.blockPosition()).value().getBaseTemperature();
                            if (biomeTemp < 0.2f) {
                                icingRate *= 2.0f; // Freeze faster in cold biomes
                            }

                            if (suit.getIcingLevel() < 100.0f) {
                                suit.setIcingLevel(suit.getIcingLevel() + icingRate);
                            }
                        } else {
                            // Thaw if low enough (slower thaw so they stay frozen for a few seconds)
                            if (suit.getIcingLevel() > 0.0f) {
                                suit.setIcingLevel(suit.getIcingLevel() - 0.2f); 
                            }
                        }

                        // Apply vanilla freezing visual effect
                        int freezeTicks = (int) ((suit.getIcingLevel() / 100.0f) * 140);
                        player.setTicksFrozen(freezeTicks);

                        // Effects of icing (applied regardless of current Y pos, strictly based on icing %)
                        if (suit.getIcingLevel() > 50.0f) {
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
                        }
                        
                        if (suit.getIcingLevel() >= 100.0f) {
                            // Freeze systems - Critical Failure
                            if (suit.isFlying()) {
                                suit.setFlying(false);
                                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cSYSTEM FAILURE: ENGINES FROZEN!"), true);
                            }
                            
                            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false, true));
                            
                            // Свободное падение 20 блоков перед экстренным планированием
                            if (player.fallDistance >= 20.0f) {
                                if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
                                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§eAUTO-DEPLOY: EMERGENCY GLIDE INITIATED!"), true);
                                }
                                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
                            }
                        }

                        changed = true; // Constantly ticking energy/icing
                    } else {
                        if (suit.getSuitTier().equals("mk2")) {
                            suit.setSuitTier("none");
                            suit.setFlying(false);
                            if (!player.isCreative() && !player.isSpectator()) {
                                player.getAbilities().mayfly = false;
                                player.getAbilities().flying = false;
                                player.onUpdateAbilities();
                            }
                            changed = true;
                        }
                    }

                    if (changed || player.tickCount % 20 == 0) {
                        ModMessages.sendToPlayer(new PacketSyncSuitData(
                                suit.getEnergy(), suit.getMaxEnergy(), suit.getSuitTier(), 
                                suit.getFrameDurability(), suit.getPalladiumPoisoning(),
                                suit.getIcingLevel(), suit.isFlying()), player);
                    }
                });
            }
        }
    }
}
