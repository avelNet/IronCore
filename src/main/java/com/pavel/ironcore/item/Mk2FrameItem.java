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

                    if (isFull) {
                        // Авто-активация STANDBY только если системы не заморожены
                        if (suit.getIcingLevel() < 100.0f) {
                            if (!suit.isFlying()) {
                                suit.setFlying(true);
                            }
                        }

                        // --- PHYSICS (Client & Server) ---
                        if (player.getAbilities().flying) {
                            boolean isBoosting = level.isClientSide ? 
                                net.minecraft.client.Minecraft.getInstance().options.keySprint.isDown() : 
                                suit.isBoostKeyHeld();

                            if (suit.getEnergy() <= 1000 && suit.getEnergy() >= 4 && suit.getIcingLevel() < 100.0f) {
                                // Резервное питание
                                Vec3 current = player.getDeltaMovement();
                                player.setDeltaMovement(current.x * 0.9, current.y - 0.1, current.z * 0.9);
                                player.hasImpulse = true;
                            } else if (isBoosting && suit.getEnergy() > 1000 && suit.getIcingLevel() < 100.0f) {
                                Vec3 look = player.getLookAngle();
                                Vec3 current = player.getDeltaMovement();
                                
                                if (current.length() < 0.3) {
                                    current = look.scale(0.3);
                                }
                                
                                double maxSpeed = 0.85; 
                                double acceleration = 0.08; 

                                // Усиление вертикальной тяги
                                double yBoost = look.y > 0 ? look.y * 1.5 : look.y;
                                Vec3 target = new Vec3(look.x, yBoost, look.z).normalize().scale(maxSpeed);
                                
                                Vec3 newMovement = new Vec3(
                                    current.x + (target.x - current.x) * acceleration,
                                    current.y + (target.y - current.y) * acceleration,
                                    current.z + (target.z - current.z) * acceleration
                                );
                                
                                player.setDeltaMovement(newMovement);
                                player.hasImpulse = true;
                            } else if (suit.getEnergy() >= 4 && suit.getIcingLevel() < 100.0f) {
                                // Ограничение скорости парения
                                Vec3 current = player.getDeltaMovement();
                                double hoverMaxSpeed = 0.28;
                                double horizontalLength = Math.sqrt(current.x * current.x + current.z * current.z);
                                if (horizontalLength > hoverMaxSpeed) {
                                    double scale = hoverMaxSpeed / horizontalLength;
                                    player.setDeltaMovement(current.x * scale, current.y, current.z * scale);
                                }
                            }
                        }
                    }

                    // --- SERVER LOGIC ---
                    if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                        boolean changed = false;

                        if (isFull) {
                            if (!suit.getSuitTier().equals("mk2")) {
                                suit.setSuitTier("mk2");
                                suit.setMaxEnergy(50000);
                                changed = true;
                            }

                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
                            
                            // Системы полета
                            boolean systemsFunctional = suit.getIcingLevel() < 100.0f && suit.getEnergy() >= 4;
                            serverPlayer.getAbilities().mayfly = systemsFunctional;
                            
                            if (serverPlayer.getAbilities().flying) {
                                if (!systemsFunctional) {
                                    serverPlayer.getAbilities().flying = false;
                                    serverPlayer.onUpdateAbilities();
                                } else {
                                    suit.setEnergy(suit.getEnergy() - 4);
                                    if (suit.getEnergy() <= 1000 && serverPlayer.tickCount % 40 == 0) {
                                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("§eWARNING: RESERVE POWER"), true);
                                    }
                                    // Particles
                                    ServerLevel serverLevel = (ServerLevel) level;
                                    Vec3 pos = serverPlayer.position();
                                    if (serverPlayer.tickCount % 2 == 0) {
                                        if (suit.getEnergy() > 1000 || serverPlayer.tickCount % 10 == 0) {
                                            serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
                                        }
                                    }
                                }
                            }
                            serverPlayer.onUpdateAbilities();

                            // Обледенение
                            double yPos = serverPlayer.getY();
                            if (yPos > 170) { // Высота 170
                                float icingRate = (float) ((yPos - 170) * 0.05f); 
                                if (level.getBiome(serverPlayer.blockPosition()).value().getBaseTemperature() < 0.2f) icingRate *= 2.0f;
                                if (suit.getIcingLevel() < 100.0f) suit.setIcingLevel(suit.getIcingLevel() + icingRate);
                            } else {
                                if (suit.getIcingLevel() > 0.0f) suit.setIcingLevel(suit.getIcingLevel() - 0.2f); 
                            }
                            serverPlayer.setTicksFrozen((int) ((suit.getIcingLevel() / 100.0f) * 140));
                            
                            if (suit.getIcingLevel() > 50.0f) serverPlayer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
                            
                            if (suit.getIcingLevel() >= 100.0f) {
                                if (suit.isFlying()) {
                                    suit.setFlying(false);
                                    serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("§cSYSTEM FAILURE: ENGINES FROZEN!"), true);
                                    changed = true;
                                }
                                
                                serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false, true));
                                
                                if (suit.getFailureYPos() < 0) {
                                    suit.setFailureYPos(serverPlayer.getY());
                                }

                                double distanceFallen = suit.getFailureYPos() - serverPlayer.getY();
                                if (distanceFallen >= 20.0 && !serverPlayer.onGround()) {
                                    // Применяем Slow Falling каждый тик после 20 блоков падения
                                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false, true));
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
