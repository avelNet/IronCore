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
            if (player.getItemBySlot(this.getType().getSlot()) == stack) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    boolean isFull = isFullSuitEquipped(player);

                    if (isFull) {
                        // Читаем данные реактора из НАГРУДНИКА
                        ItemStack chestplate = player.getItemBySlot(EquipmentSlot.CHEST);
                        net.minecraft.nbt.CompoundTag chestTag = chestplate.getOrCreateTag();
                        
                        String installedReactor = chestTag.getString("InstalledReactor");
                        int suitEnergy = chestTag.getInt("SuitEnergy");
                        int suitMaxEnergy = chestTag.getInt("SuitMaxEnergy");

                        // Если реактора нет - выключаем системы
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

                        // Если реактор установлен
                        if (installedReactor.contains("palladium")) suit.setActiveReactorType("palladium");
                        else if (installedReactor.contains("coal")) suit.setActiveReactorType("coal");
                        
                        suit.setMaxEnergy(suitMaxEnergy);
                        if (suit.getEnergy() != suitEnergy) {
                           suit.setEnergy(suitEnergy); 
                        }

                        // Авто-активация STANDBY (Mk3 не боится льда, проверяем только перегрев)
                        if (suit.getHeat() < 100.0f) {
                            if (!suit.isFlying()) {
                                suit.setFlying(true);
                            }
                        }

                        // --- PHYSICS (Client & Server) ---
                        if (player.getAbilities().flying) {
                            boolean isBoosting = level.isClientSide ? 
                                net.minecraft.client.Minecraft.getInstance().options.keySprint.isDown() : 
                                suit.isBoostKeyHeld();

                            boolean enginesOverheated = suit.getHeat() >= 100.0f;

                            if (player.isInWater()) {
                                player.getAbilities().flying = false;
                                player.onUpdateAbilities();
                            } else if (enginesOverheated) {
                                Vec3 current = player.getDeltaMovement();
                                player.setDeltaMovement(current.x * 0.9, current.y - 0.1, current.z * 0.9);
                                player.hasImpulse = true;
                            } else if (suit.getEnergy() <= 1000 && suit.getEnergy() >= 4) {
                                Vec3 current = player.getDeltaMovement();
                                player.setDeltaMovement(current.x * 0.9, current.y - 0.1, current.z * 0.9);
                                player.hasImpulse = true;
                            } else if (isBoosting && suit.getEnergy() > 1000) {
                                Vec3 look = player.getLookAngle();
                                Vec3 current = player.getDeltaMovement();
                                if (current.length() < 0.3) current = look.scale(0.3);
                                
                                // MK3 SPEED (120 km/h = ~1.7 blocks/tick)
                                double maxSpeed = 1.7; 
                                double accelerationXZ = 0.12; 
                                double accelerationY = 0.3; // Сильнее толкаем по Y, чтобы пробить ванильное сопротивление полета
                                
                                double yBoost = look.y * 2.0; // Усиливаем вектор взгляда по вертикали
                                Vec3 target = new Vec3(look.x, yBoost, look.z).normalize().scale(maxSpeed);
                                
                                Vec3 newMovement = new Vec3(
                                    current.x + (target.x - current.x) * accelerationXZ,
                                    current.y + (target.y - current.y) * accelerationY,
                                    current.z + (target.z - current.z) * accelerationXZ
                                );
                                
                                player.setDeltaMovement(newMovement);
                                player.hasImpulse = true;
                            } else if (suit.getEnergy() >= 4) {
                                // Ограничение скорости парения
                                Vec3 current = player.getDeltaMovement();
                                double hoverMaxSpeed = 0.4; // Чуть быстрее парит чем Mk2
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

                    // --- SERVER LOGIC ---
                    if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                        boolean changed = false;

                        if (isFull) {
                            if (!suit.getSuitTier().equals("mk3")) {
                                suit.setSuitTier("mk3");
                                changed = true;
                            }

                            // Пассивные баффы Mk3
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, false, true)); // Сила 2
                            serverPlayer.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false, true)); // Сопротивление 2
                            
                            // Системы полета
                            boolean systemsFunctional = suit.getHeat() < 100.0f && suit.getEnergy() >= 4 && !serverPlayer.isInWater();
                            serverPlayer.getAbilities().mayfly = systemsFunctional;
                            
                            if (serverPlayer.getAbilities().flying) {
                                if (!systemsFunctional) {
                                    serverPlayer.getAbilities().flying = false;
                                    if (serverPlayer.isInWater()) serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("§cSYSTEM FAILURE: WATER DETECTED!"), true);
                                    else if (suit.getHeat() >= 100.0f) serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("§cSYSTEM FAILURE: OVERHEATED!"), true);
                                    serverPlayer.onUpdateAbilities();
                                } else {
                                    suit.setEnergy(suit.getEnergy() - 5); // Потребляет чуть больше энергии
                                    
                                    ItemStack chestplateServer = serverPlayer.getItemBySlot(EquipmentSlot.CHEST);
                                    if(chestplateServer.getItem() instanceof ArmorItem) {
                                         chestplateServer.getOrCreateTag().putInt("SuitEnergy", suit.getEnergy()); 
                                    }

                                    // Охлаждение лучше, нагревается медленнее (0.025 вместо 0.047)
                                    if (suit.isBoostKeyHeld()) suit.setHeat(suit.getHeat() + 0.025f);
                                    
                                    if (suit.getEnergy() <= 1000 && serverPlayer.tickCount % 40 == 0) {
                                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("§eWARNING: RESERVE POWER"), true);
                                    }
                                    
                                    ServerLevel serverLevel = (ServerLevel) level;
                                    Vec3 pos = serverPlayer.position();
                                    if (serverPlayer.tickCount % 2 == 0) {
                                        if (suit.getEnergy() > 1000 || serverPlayer.tickCount % 10 == 0) {
                                            // Синее пламя репульсоров
                                            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, pos.x, pos.y + 0.1, pos.z, 1, 0.1, 0.0, 0.1, 0.02);
                                        }
                                    }
                                }
                            }
                            serverPlayer.onUpdateAbilities();

                            // ОБЛЕДЕНЕНИЕ ОТСУТСТВУЕТ У MK3 (Золото-титан решает проблему)
                            if (suit.getIcingLevel() > 0.0f) {
                                suit.setIcingLevel(suit.getIcingLevel() - 0.5f); // Быстро оттаивает, если было
                            }

                            // Система Перегрева (Охлаждается быстрее)
                            float coolingRate = 0.2f;
                            if (serverPlayer.isInWater()) coolingRate = 0.8f;
                            else if (level.getBiome(serverPlayer.blockPosition()).value().getBaseTemperature() < 0.2f) coolingRate = 0.4f;
                            if (level.dimension() == net.minecraft.world.level.Level.NETHER) suit.setHeat(suit.getHeat() + 0.03f);
                            if (serverPlayer.isOnFire() || serverPlayer.isInLava()) suit.setHeat(suit.getHeat() + 0.5f); // Горит в 2 раза медленнее
                            if (!suit.isBoostKeyHeld() || !serverPlayer.getAbilities().flying) {
                                suit.setHeat(suit.getHeat() - coolingRate);
                            }
                            if (suit.getHeat() > 80.0f && serverPlayer.tickCount % 40 == 0) {
                                serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.literal("§6WARNING: CRITICAL TEMPERATURE!"), true);
                            }

                            if (suit.getHeat() >= 100.0f) {
                                if (serverPlayer.getAbilities().flying) {
                                    serverPlayer.getAbilities().flying = false;
                                    serverPlayer.onUpdateAbilities();
                                }
                                serverPlayer.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, false, false, true));
                                if (suit.getFailureYPos() < 0) suit.setFailureYPos(serverPlayer.getY());
                                if (suit.getFailureYPos() - serverPlayer.getY() >= 20.0 && !serverPlayer.onGround()) {
                                    serverPlayer.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, false, false, true));
                                }
                            } else {
                                if (suit.getFailureYPos() >= 0) suit.setFailureYPos(-1.0);
                            }

                            serverPlayer.setTicksFrozen(0); // Никогда не замерзает визуально
                            changed = true;
                        } else {
                            if (suit.getSuitTier().equals("mk3")) {
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
                                    suit.getFrameDurability(), suit.getPalladiumPoisoning(), suit.getActiveReactorType(),
                                    suit.getIcingLevel(), suit.getHeat(), suit.isFlying()), serverPlayer);
                        }
                    }
                });
            }
        }
    }
}
