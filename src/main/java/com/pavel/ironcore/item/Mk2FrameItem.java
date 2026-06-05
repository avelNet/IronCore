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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Mk2FrameItem extends ArmorItem {
    public Mk2FrameItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    private boolean isFullSuitEquipped(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.MK2_HELMET.get() &&
               player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.MK2_CHESTPLATE.get() &&
               player.getItemBySlot(EquipmentSlot.LEGS).getItem() == ModItems.MK2_LEGGINGS.get() &&
               player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.MK2_BOOTS.get();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player) {
            if (player.getItemBySlot(this.getType().getSlot()) == stack) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    boolean isFull = isFullSuitEquipped(player);

                    // --- CLIENT & SERVER (Для плавной физики без фризов) ---
                    if (isFull) {
                        if (suit.isFlying() && player.getAbilities().flying) {
                            if (player.isSprinting() && suit.getEnergy() >= 4 && suit.getIcingLevel() < 100.0f) {
                                Vec3 look = player.getLookAngle();
                                Vec3 current = player.getDeltaMovement();
                                
                                // Target speed ~60 km/h (approx 0.85 blocks/tick)
                                double maxSpeed = 0.85; 
                                double acceleration = 0.15; // Faster responsiveness

                                Vec3 target = look.scale(maxSpeed);
                                
                                // Smooth interpolation
                                Vec3 newMovement = new Vec3(
                                    current.x + (target.x - current.x) * acceleration,
                                    current.y + (target.y - current.y) * acceleration,
                                    current.z + (target.z - current.z) * acceleration
                                );
                                
                                player.setDeltaMovement(newMovement);
                            }
                        }
                    }

                    // --- СЕРВЕРНАЯ ЧАСТЬ (Энергия, Баффы, Синхронизация, Обледенение) ---
                    if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                        boolean changed = false;

                        if (isFull) {
                            if (!suit.getSuitTier().equals("mk2")) {
                                suit.setSuitTier("mk2");
                                suit.setMaxEnergy(50000);
                                changed = true;
                            }

                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));

                            if (suit.isFlying()) {
                                serverPlayer.getAbilities().mayfly = true;
                                
                                if (serverPlayer.getAbilities().flying) {
                                    if (suit.getEnergy() >= 4 && suit.getIcingLevel() < 100.0f) {
                                        suit.setEnergy(suit.getEnergy() - 4);
                                        
                                        // Particles
                                        ServerLevel serverLevel = (ServerLevel) level;
                                        Vec3 pos = serverPlayer.position();
                                        if (serverPlayer.tickCount % 2 == 0) {
                                            serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
                                        }
                                    } else {
                                        suit.setFlying(false);
                                        if (!serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
                                            serverPlayer.getAbilities().mayfly = false;
                                            serverPlayer.getAbilities().flying = false;
                                        }
                                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("§cSYSTEM FAILURE: Flight disabled!"), true);
                                        changed = true;
                                    }
                                }
                                serverPlayer.onUpdateAbilities();
                            } else {
                                if (!serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
                                    if (serverPlayer.getAbilities().mayfly) {
                                        serverPlayer.getAbilities().mayfly = false;
                                        serverPlayer.getAbilities().flying = false;
                                        serverPlayer.onUpdateAbilities();
                                    }
                                }
                            }

                            // Icing mechanics
                            double yPos = serverPlayer.getY();
                            if (yPos > 200) {
                                float icingRate = (float) ((yPos - 200) * 0.05f); 
                                float biomeTemp = level.getBiome(serverPlayer.blockPosition()).value().getBaseTemperature();
                                if (biomeTemp < 0.2f) {
                                    icingRate *= 2.0f;
                                }

                                if (suit.getIcingLevel() < 100.0f) {
                                    suit.setIcingLevel(suit.getIcingLevel() + icingRate);
                                }
                            } else {
                                if (suit.getIcingLevel() > 0.0f) {
                                    suit.setIcingLevel(suit.getIcingLevel() - 0.2f); 
                                }
                            }

                            int freezeTicks = (int) ((suit.getIcingLevel() / 100.0f) * 140);
                            serverPlayer.setTicksFrozen(freezeTicks);

                            if (suit.getIcingLevel() > 50.0f) {
                                serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
                            }
                            
                            if (suit.getIcingLevel() >= 100.0f) {
                                if (suit.isFlying()) {
                                    suit.setFlying(false);
                                    serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("§cSYSTEM FAILURE: ENGINES FROZEN!"), true);
                                }
                                
                                serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false, true));
                                
                                if (suit.getFailureYPos() < 0) {
                                    suit.setFailureYPos(serverPlayer.getY());
                                }

                                double distanceFallen = suit.getFailureYPos() - serverPlayer.getY();

                                if (distanceFallen >= 20.0 && !serverPlayer.onGround()) {
                                    if (!serverPlayer.hasEffect(MobEffects.SLOW_FALLING)) {
                                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("§eAUTO-DEPLOY: EMERGENCY GLIDE INITIATED!"), true);
                                    }
                                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
                                }
                            } else {
                                if (suit.getFailureYPos() >= 0) {
                                    suit.setFailureYPos(-1.0);
                                }
                            }

                            changed = true;
                        } else {
                            if (suit.getSuitTier().equals("mk2")) {
                                suit.setSuitTier("none");
                                suit.setFlying(false);
                                if (!serverPlayer.isCreative() && !serverPlayer.isSpectator()) {
                                    serverPlayer.getAbilities().mayfly = false;
                                    serverPlayer.getAbilities().flying = false;
                                    serverPlayer.onUpdateAbilities();
                                }
                                changed = true;
                            }
                        }

                        if (changed || serverPlayer.tickCount % 20 == 0) {
                            ModMessages.sendToPlayer(new PacketSyncSuitData(
                                    suit.getEnergy(), suit.getMaxEnergy(), suit.getSuitTier(), 
                                    suit.getFrameDurability(), suit.getPalladiumPoisoning(),
                                    suit.getIcingLevel(), suit.isFlying()), serverPlayer);
                        }
                    }
                });
            }
        }
    }
}
