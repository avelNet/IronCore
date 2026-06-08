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

public class Mk3FrameItem extends ArmorItem {
    public Mk3FrameItem(ArmorMaterial material, ArmorItem.Type type, Properties properties) {
        super(material, type, properties);
    }

    private boolean isFullSuitEquipped(Player player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == ModItems.MK3_HELMET.get() &&
               player.getItemBySlot(EquipmentSlot.CHEST).getItem() == ModItems.MK3_CHESTPLATE.get() &&
               player.getItemBySlot(EquipmentSlot.LEGS).getItem() == ModItems.MK3_LEGGINGS.get() &&
               player.getItemBySlot(EquipmentSlot.FEET).getItem() == ModItems.MK3_BOOTS.get();
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (entity instanceof Player player) {
            // Ограничиваем выполнение логики только нагрудником, чтобы она не срабатывала 4 раза за тик
            if (this.getType() == ArmorItem.Type.CHESTPLATE && player.getItemBySlot(EquipmentSlot.CHEST) == stack) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    boolean isFull = isFullSuitEquipped(player);

                    if (isFull) {
                        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
                        net.minecraft.nbt.CompoundTag chestTag = chestplate.getOrCreateTag();
                        
                        String installedReactor = chestTag.getString("InstalledReactor");
                        int suitEnergy = chestTag.getInt("SuitEnergy");
                        int suitMaxEnergy = chestTag.getInt("SuitMaxEnergy");

                        if (installedReactor.isEmpty() || installedReactor.equals("none")) {
                            suit.setActiveReactorType("none");
                            suit.setEnergy(0);
                            suit.setMaxEnergy(0);
                            if (suit.isFlying()) suit.setFlying(false);
                            if (player.getAbilities().flying) {
                                player.getAbilities().flying = false;
                                player.onUpdateAbilities();
                            }
                            if (!level.isClientSide && player.tickCount % 20 == 0) {
                                ModMessages.sendToPlayer(new PacketSyncSuitData(
                                    suit.getEnergy(), suit.getMaxEnergy(), suit.getSuitTier(), 
                                    suit.getFrameDurability(), suit.getPalladiumPoisoning(), suit.getActiveReactorType(),
                                    suit.getIcingLevel(), suit.getHeat(), suit.isFlying()), (ServerPlayer)player);
                            }
                            return; 
                        }

                        if (installedReactor.contains("palladium")) suit.setActiveReactorType("palladium");
                        else if (installedReactor.contains("coal")) suit.setActiveReactorType("coal");
                        
                        suit.setMaxEnergy(suitMaxEnergy);
                        if (suit.getEnergy() != suitEnergy) {
                           suit.setEnergy(suitEnergy); 
                        }

                        if (suit.getHeat() < 100.0f) {
                            if (!suit.isFlying()) {
                                suit.setFlying(true);
                            }
                        }

                        if (player.getAbilities().flying) {
                            boolean isBoosting = level.isClientSide ? 
                                net.minecraft.client.Minecraft.getInstance().options.keySprint.isDown() : 
                                suit.isBoostKeyHeld();

                            boolean enginesFrozen = suit.getIcingLevel() >= 100.0f;
                            boolean enginesOverheated = suit.getHeat() >= 100.0f;

                            if (player.isInWater()) {
                                player.getAbilities().flying = false;
                                player.onUpdateAbilities();
                            } else if (enginesOverheated) {
                                Vec3 current = player.getDeltaMovement();
                                player.setDeltaMovement(current.x * 0.9, current.y - 0.1, current.z * 0.9);
                                player.hasImpulse = true;
                            } else if (suit.getEnergy() <= 1000 && suit.getEnergy() >= 4 && !enginesFrozen) {
                                Vec3 current = player.getDeltaMovement();
                                player.setDeltaMovement(current.x * 0.9, current.y - 0.1, current.z * 0.9);
                                player.hasImpulse = true;
                            } else if (isBoosting && suit.getEnergy() > 1000 && !enginesFrozen) {
                                Vec3 look = player.getLookAngle();
                                Vec3 current = player.getDeltaMovement();
                                
                                double maxSpeed = 1.6; // ~115 km/h
                                double currentSpeed = current.length();
                                double speedRatio = Math.min(currentSpeed / maxSpeed, 1.0);
                                
                                // Стабильное ускорение от 0.15 (старт) до 0.75 (взрыв).
                                // Коэффициент НИКОГДА не должен быть > 1.0, иначе происходит перерегулирование (баг на 250 км/ч)
                                double acceleration = 0.15 + Math.pow(speedRatio, 2.0) * 0.6; 
                                
                                Vec3 target = look.scale(maxSpeed);
                                double targetY = target.y;
                                
                                if (targetY > 0) {
                                    // +0.1 дает жесткий пинок вверх, чтобы мгновенно пробить стену гравитации
                                    targetY = targetY * 1.2 + 0.1; 
                                } else if (targetY < 0) {
                                    targetY = targetY * 1.4; // Контролируемое пикирование (~150 км/ч)
                                }
                                
                                Vec3 newMovement = new Vec3(
                                    current.x + (target.x - current.x) * acceleration,
                                    current.y + (targetY - current.y) * acceleration,
                                    current.z + (target.z - current.z) * acceleration
                                );
                                
                                player.setDeltaMovement(newMovement);
                                player.hasImpulse = true;
                            } else if (suit.getEnergy() >= 4 && !enginesFrozen) {
                                Vec3 current = player.getDeltaMovement();
                                double hoverMaxSpeed = 0.4; 
                                if (current.length() > hoverMaxSpeed) {
                                    player.setDeltaMovement(current.scale(0.85)); 
                                    if (player.getDeltaMovement().length() > hoverMaxSpeed) {
                                        player.setDeltaMovement(player.getDeltaMovement().normalize().scale(hoverMaxSpeed));
                                    }
                                    player.hasImpulse = true;
                                }
                            }
                        }
                    }

                    if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                        boolean changed = false;

                        if (isFull) {
                            if (!suit.getSuitTier().equals("mk3")) {
                                suit.setSuitTier("mk3");
                                suit.setMaxEnergy(100000);
                                changed = true;
                            }

                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, false, true));
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false, true));
                            
                            boolean systemsFunctional = suit.getIcingLevel() < 100.0f && suit.getHeat() < 100.0f && suit.getEnergy() >= 4 && !serverPlayer.isInWater();
                            serverPlayer.getAbilities().mayfly = systemsFunctional;
                            
                            if (serverPlayer.getAbilities().flying) {
                                if (!systemsFunctional) {
                                    serverPlayer.getAbilities().flying = false;
                                    serverPlayer.onUpdateAbilities();
                                } else {
                                    if (serverPlayer.tickCount % 2 == 0) {
                                        suit.setEnergy(suit.getEnergy() - 1);
                                    }
                                    
                                    ItemStack chestplateServer = serverPlayer.getItemBySlot(EquipmentSlot.CHEST);
                                    if(chestplateServer.getItem() instanceof ArmorItem) {
                                         chestplateServer.getOrCreateTag().putInt("SuitEnergy", suit.getEnergy()); 
                                    }

                                    if (suit.isBoostKeyHeld()) suit.setHeat(suit.getHeat() + 0.025f);
                                    
                                    ServerLevel serverLevel = (ServerLevel) level;
                                    Vec3 pos = serverPlayer.position();
                                    if (serverPlayer.tickCount % 2 == 0) {
                                        if (suit.getEnergy() > 1000 || serverPlayer.tickCount % 10 == 0) {
                                            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
                                        }
                                    }
                                }
                            }
                            serverPlayer.onUpdateAbilities();

                            if (suit.getIcingLevel() > 0.0f) {
                                suit.setIcingLevel(suit.getIcingLevel() - 0.5f);
                            }

                            float coolingRate = 0.2f;
                            if (serverPlayer.isInWater()) coolingRate = 0.8f;
                            else if (level.getBiome(serverPlayer.blockPosition()).value().getBaseTemperature() < 0.2f) coolingRate = 0.4f;
                            if (level.dimension() == net.minecraft.world.level.Level.NETHER) suit.setHeat(suit.getHeat() + 0.03f);
                            if (serverPlayer.isOnFire() || serverPlayer.isInLava()) suit.setHeat(suit.getHeat() + 0.5f);
                            if (!suit.isBoostKeyHeld() || !serverPlayer.getAbilities().flying) {
                                suit.setHeat(suit.getHeat() - coolingRate);
                            }

                            if (suit.getHeat() >= 100.0f) {
                                if (serverPlayer.getAbilities().flying) {
                                    serverPlayer.getAbilities().flying = false;
                                    serverPlayer.onUpdateAbilities();
                                }
                                serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false, true));
                            }

                            serverPlayer.setTicksFrozen(0);
                            changed = true;
                        } else {
                            if (suit.getSuitTier().equals("mk3")) {
                                suit.setSuitTier("none");
                                suit.setActiveReactorType("none");
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
                                    suit.getFrameDurability(), suit.getPalladiumPoisoning(), suit.getActiveReactorType(),
                                    suit.getIcingLevel(), suit.getHeat(), suit.isFlying()), serverPlayer);
                        }
                    }
                });
            }
        }
    }
}
