package com.pavel.ironcore;

import com.pavel.ironcore.capability.SuitCapability;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.client.KeyBindings;
import com.pavel.ironcore.item.ModCreativeTabs;
import com.pavel.ironcore.item.ModItems;
import com.pavel.ironcore.network.ModMessages;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import com.pavel.ironcore.block.ModBlocks;
import com.pavel.ironcore.block.entity.ModBlockEntities;

import com.pavel.ironcore.screen.AlloySmelterScreen;
import com.pavel.ironcore.screen.AssemblyTableScreen;
import com.pavel.ironcore.screen.ChargingStationScreen;
import com.pavel.ironcore.screen.CoalGeneratorScreen;
import com.pavel.ironcore.screen.SuitStationScreen;
import com.pavel.ironcore.screen.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;

@Mod(IronCore.MODID)
public class IronCore {
    public static final String MODID = "ironcore";

    public IronCore() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModMessages::register);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        KeyBindings.init();
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.ALLOY_SMELTER_MENU.get(), AlloySmelterScreen::new);
            MenuScreens.register(ModMenuTypes.COAL_GENERATOR_MENU.get(), CoalGeneratorScreen::new);
            MenuScreens.register(ModMenuTypes.ASSEMBLY_TABLE_MENU.get(), AssemblyTableScreen::new);
            MenuScreens.register(ModMenuTypes.CHARGING_STATION_MENU.get(), ChargingStationScreen::new);
            MenuScreens.register(ModMenuTypes.SUIT_STATION_MENU.get(), SuitStationScreen::new);
        });
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onKeyRegister(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.FLAMETHROWER_KEY);
            event.register(KeyBindings.AUTO_BOOST_KEY);
            event.register(KeyBindings.TOGGLE_MASK_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class ForgeEvents {
        
        @SubscribeEvent
        public static void onEntitySize(net.minecraftforge.event.entity.EntityEvent.Size event) {
            if (event.getEntity() instanceof Player player && player.getAbilities() != null && player.getAbilities().flying && !player.onGround()) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    if (suit.wasFlyingHorizontally()) {
                        // Shrink hitbox height to allow flying through 1-block gaps
                        event.setNewSize(net.minecraft.world.entity.EntityDimensions.fixed(0.6f, 0.6f), false);
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
            if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
                event.player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    
                    // Client-side & Server-side horizontal flight detection for dimension refreshing
                    boolean isFlying = event.player.getAbilities().flying && !event.player.onGround();
                    boolean isBoosting = false;
                    
                    if (event.player.level().isClientSide()) {
                        // Client: check key
                        isBoosting = net.minecraft.client.Minecraft.getInstance().options.keySprint.isDown();
                    } else {
                        // Server: check stored state or speed
                        isBoosting = suit.isBoostKeyHeld() || event.player.getDeltaMovement().length() > 0.35;
                    }

                    boolean isFlyingHorizontally = isFlying && isBoosting && event.player.getDeltaMovement().length() > 0.1;
                    
                    if (isFlyingHorizontally != suit.wasFlyingHorizontally()) {
                        suit.setWasFlyingHorizontally(isFlyingHorizontally);
                        event.player.refreshDimensions();
                        if (!event.player.level().isClientSide()) {
                            ModMessages.sendSyncPacket((net.minecraft.server.level.ServerPlayer) event.player);
                        }
                    }

                    if (event.player.level().isClientSide()) return; // Server only logic follows
                    ServerPlayer serverPlayer = (ServerPlayer) event.player;

                    // Проверка инвентаря на наличие токсичных отходов
                    boolean hasDepletedCore = event.player.getInventory().contains(new net.minecraft.world.item.ItemStack(ModItems.DEPLETED_PALLADIUM_CORE.get()));

                    // ── Логика отравления палладием ───────────────────────────────────────────
                    // Ставки подобраны так чтобы угроза нарастала часами, а не минутами.
                    // Ориентир: активный boost-полёт с embedded реактором → ~4.6 ч до 100%.
                    //           первые часы вообще без костюма → отравление практически нулевое.
                    //
                    // Всё в единицах/тик (1 тик = 1/20 сек, 1200 тиков = 1 мин):
                    //   embedded boost   0.0003  →  0.36%/мин  →  ~4.6 ч
                    //   embedded hover   0.0001  →  0.12%/мин  →  ~14 ч
                    //   embedded ground  0.00005 →  0.06%/мин  →  ~28 ч
                    //   нет костюма, есть имплант: 0.000002   →  ~1400 ч (незаметно)
                    //   отработанный стержень    +0.001       →  +72%/ч (сильный штраф)
                    //   естественный спад без костюма: −0.00003/тик (~0.04%/мин)

                    boolean activelyFlying = event.player.getAbilities().flying && !event.player.onGround();
                    boolean activelBoosting = suit.isBoostKeyHeld() && activelyFlying;

                    float poisonRate = 0.0f;

                    if (suit.hasEmbeddedReactor()) {
                        if (suit.getSuitTier().equals("none")) {
                            // Имплант есть, но костюм снят — реактор дормантный, почти ноль
                            poisonRate = 0.000002f;
                        } else if (activelBoosting) {
                            poisonRate = 0.0003f;
                        } else if (activelyFlying) {
                            poisonRate = 0.0001f;
                        } else {
                            poisonRate = 0.00005f;
                        }
                    }

                    if (hasDepletedCore) {
                        poisonRate += 0.001f; // Сильный штраф за ношение отработанного стержня
                    }

                    if (poisonRate > 0) {
                        suit.setPalladiumPoisoning(Math.min(100.0f, suit.getPalladiumPoisoning() + poisonRate));

                        if (event.player.tickCount % 20 == 0) {
                            ModMessages.sendSyncPacket(serverPlayer);
                        }

                        if (event.player.tickCount % 40 == 0) {
                            if (suit.getPalladiumPoisoning() > 30.0f) {
                                event.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 80, 0, false, false, true));
                            }
                            if (suit.getPalladiumPoisoning() > 60.0f) {
                                event.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 0, false, false, true));
                            }
                        }
                        if (suit.getPalladiumPoisoning() > 85.0f) {
                            if (event.player.tickCount % 100 == 0) {
                                event.player.hurt(event.player.damageSources().magic(), 1.0f);
                            }
                        }
                    } else if (suit.getPalladiumPoisoning() > 0) {
                        // Естественный спад — очень медленный, нужен хлорофилловый сок для лечения
                        suit.setPalladiumPoisoning(Math.max(0.0f, suit.getPalladiumPoisoning() - 0.00003f));
                        if (event.player.tickCount % 20 == 0) {
                            ModMessages.sendSyncPacket(serverPlayer);
                        }
                    }

                    if (!suit.getSuitTier().equals("none")) {
                        // Проверка-предохранитель на случай если броню удалили читом (Clear Inventory)
                        boolean hasMk1 = event.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem() == ModItems.MK1_CHESTPLATE.get();
                        boolean hasMk2 = event.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem() == ModItems.MK2_CHESTPLATE.get();
                        boolean hasMk3 = event.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).getItem() == ModItems.MK3_CHESTPLATE.get();
                        
                        if (!hasMk1 && !hasMk2 && !hasMk3) {
                            suit.setSuitTier("none");
                            suit.setActiveReactorType("none");
                            suit.setEnergy(0);
                            suit.setMaxEnergy(0);
                            suit.setFlying(false);
                            if (!event.player.isCreative() && !event.player.isSpectator()) {
                                event.player.getAbilities().mayfly = false;
                                event.player.getAbilities().flying = false;
                                event.player.onUpdateAbilities();
                            }
                            ModMessages.sendSyncPacket(serverPlayer);
                        }
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onLivingFall(LivingFallEvent event) {
            if (event.getEntity() instanceof Player player) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    // Системы должны быть активны (есть энергия и нет критического обледенения)
                    if ((suit.getSuitTier().equals("mk1") || suit.getSuitTier().equals("mk2")) 
                        && suit.getEnergy() > 0 && suit.getIcingLevel() < 100.0f) {
                        
                        float damage = event.getDistance() - 3.0f; // Ванильный порог урона
                        if (damage > 0) {
                            int energyCost = (int) (damage * 200); // 200 FE за каждое сердце урона
                            
                            if (suit.getEnergy() >= energyCost) {
                                suit.setEnergy(suit.getEnergy() - energyCost);
                                event.setDistance(0); // Отменяем урон
                                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§bSHOCK ABSORBERS: Impact mitigated!"), true);
                            } else {
                                // Если энергии мало, поглощаем сколько можем
                                float absorbed = suit.getEnergy() / 200.0f;
                                suit.setEnergy(0);
                                event.setDistance(event.getDistance() - absorbed);
                                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cSHOCK ABSORBERS: Energy depleted!"), true);
                            }
                        }
                    }
                });
            }
        }

        @SubscribeEvent
        public static void onAttachCapabilitiesPlayer(AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof Player) {
                if (!event.getObject().getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).isPresent()) {
                    event.addCapability(new ResourceLocation(MODID, "suit_properties"), new SuitCapabilityProvider());
                }
            }
        }

        @SubscribeEvent
        public static void onPlayerClone(PlayerEvent.Clone event) {
            event.getOriginal().reviveCaps();
            event.getOriginal().getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(oldSuit -> {
                event.getEntity().getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(newSuit -> {
                    // Copy only "body-permanent" data — implant, poisoning, story flags, preferences.
                    // Suit tier, energy, heat etc. reset naturally (armor drops on death).
                    newSuit.setEmbeddedReactor(oldSuit.hasEmbeddedReactor());
                    newSuit.setPalladiumPoisoning(oldSuit.getPalladiumPoisoning());
                    newSuit.setJarvisUnlocked(oldSuit.isJarvisUnlocked());
                    newSuit.setAutoBoostEnabled(oldSuit.isAutoBoostEnabled());
                    newSuit.setFirstNightTriggered(oldSuit.isFirstNightTriggered());
                    newSuit.setCinematicStage(oldSuit.getCinematicStage());
                });
            });
            event.getOriginal().invalidateCaps();
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(SuitCapability.class);
        }
    }
}
