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

                        // Каноничный бафф (наследуется от Mk1)
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));

                        // Flight mechanics
                        if (suit.isFlying()) {
                            player.getAbilities().mayfly = true;
                            
                            // Расход энергии только когда игрок АКТИВНО летит (в воздухе)
                            if (player.getAbilities().flying) {
                                if (suit.getEnergy() >= 4 && suit.getIcingLevel() < 100.0f) {
                                    suit.setEnergy(suit.getEnergy() - 4); 
                                    
                                    // Fast horizontal flight if sprinting
                                    if (player.isSprinting()) {
                                        Vec3 look = player.getLookAngle();
                                        Vec3 currentMovement = player.getDeltaMovement();

                                        // 60 km/h = 16.66 m/s = 0.833 m/tick
                                        double maxSpeed = 0.833;
                                        double acceleration = 0.08; // Gradual acceleration

                                        // Target velocity is look direction * max speed
                                        Vec3 targetMovement = look.scale(maxSpeed);

                                        // Interpolate current movement towards target movement
                                        Vec3 newMovement = new Vec3(
                                            currentMovement.x + (targetMovement.x - currentMovement.x) * acceleration,
                                            currentMovement.y + (targetMovement.y - currentMovement.y) * acceleration,
                                            currentMovement.z + (targetMovement.z - currentMovement.z) * acceleration
                                        );

                                        player.setDeltaMovement(newMovement);
                                        player.hurtMarked = true; // Tell client to update velocity
                                        suit.setEnergy(suit.getEnergy() - 6); // Extra energy for boosting
                                    }
                                    
                                    // Server-side particles for thrusters
                                    ServerLevel serverLevel = (ServerLevel) level;
                                    Vec3 pos = player.position();
                                    if (player.tickCount % 2 == 0) {
                                        serverLevel.sendParticles(ParticleTypes.FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
                                    }
                                } else {
                                    // Отказ систем
                                    suit.setFlying(false);
                                    if (!player.isCreative() && !player.isSpectator()) {
                                        player.getAbilities().mayfly = false;
                                        player.getAbilities().flying = false;
                                    }
                                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cSYSTEM FAILURE: Flight disabled!"), true);
                                    changed = true;
                                }
                            }
                            player.onUpdateAbilities();
                        } else {
                            if (!player.isCreative() && !player.isSpectator()) {
                                if (player.getAbilities().mayfly) {
                                    player.getAbilities().mayfly = false;
                                    player.getAbilities().flying = false;
                                    player.onUpdateAbilities();
                                }
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
                            
                            // Record the Y position when failure starts if not already recorded
                            if (suit.getFailureYPos() < 0) {
                                suit.setFailureYPos(player.getY());
                            }

                            // Calculate actual distance fallen since failure
                            double distanceFallen = suit.getFailureYPos() - player.getY();

                            // Свободное падение 20 блоков перед экстренным планированием
                            // Убедимся, что игрок не стоит на земле (чтобы парашют не открывался, если он просто поднялся пешком)
                            if (distanceFallen >= 20.0 && !player.onGround()) {
                                if (!player.hasEffect(MobEffects.SLOW_FALLING)) {
                                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§eAUTO-DEPLOY: EMERGENCY GLIDE INITIATED!"), true);
                                }
                                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
                            }
                        } else {
                            // Reset failure Y pos when systems thaw enough
                            if (suit.getFailureYPos() >= 0) {
                                suit.setFailureYPos(-1.0);
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
