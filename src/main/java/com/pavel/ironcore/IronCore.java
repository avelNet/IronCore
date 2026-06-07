package com.pavel.ironcore;

import com.pavel.ironcore.capability.SuitCapability;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.client.KeyBindings;
import com.pavel.ironcore.client.SuitHUD;
import com.pavel.ironcore.item.ModCreativeTabs;
import com.pavel.ironcore.item.ModItems;
import com.pavel.ironcore.network.ModMessages;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
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
            event.register(KeyBindings.flamethrowerKey);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID)
    public static class ForgeEvents {
        @SubscribeEvent
        public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
            if (event.phase == net.minecraftforge.event.TickEvent.Phase.END && !event.player.level().isClientSide()) {
                event.player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    
                    // Глобальная логика отравления палладием (работает всегда)
                    if (suit.getActiveReactorType().equals("palladium")) {
                        suit.setPalladiumPoisoning(suit.getPalladiumPoisoning() + 0.01f);
                        if (suit.getPalladiumPoisoning() > 30.0f) {
                            event.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.WEAKNESS, 40, 0, false, false, true));
                        }
                        if (suit.getPalladiumPoisoning() > 60.0f) {
                            event.player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
                        }
                        if (suit.getPalladiumPoisoning() > 85.0f) {
                            if (event.player.tickCount % 100 == 0) {
                                event.player.hurt(event.player.damageSources().magic(), 1.0f);
                            }
                        }
                    } else if (suit.getPalladiumPoisoning() > 0) {
                        suit.setPalladiumPoisoning(suit.getPalladiumPoisoning() - 0.005f);
                        // Если костюм снят, но токсичность есть - синхронизируем HUD, чтобы шкала пропадала плавно
                        if (suit.getSuitTier().equals("none") && event.player.tickCount % 20 == 0) {
                            ModMessages.sendToPlayer(new com.pavel.ironcore.network.PacketSyncSuitData(
                                    suit.getEnergy(), suit.getMaxEnergy(), suit.getSuitTier(), 
                                    suit.getFrameDurability(), suit.getPalladiumPoisoning(), suit.getActiveReactorType(),
                                    suit.getIcingLevel(), suit.getHeat(), suit.isFlying()), (net.minecraft.server.level.ServerPlayer)event.player);
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
                            ModMessages.sendToPlayer(new com.pavel.ironcore.network.PacketSyncSuitData(
                                    suit.getEnergy(), suit.getMaxEnergy(), suit.getSuitTier(), 
                                    suit.getFrameDurability(), suit.getPalladiumPoisoning(), suit.getActiveReactorType(),
                                    suit.getIcingLevel(), suit.getHeat(), suit.isFlying()), (net.minecraft.server.level.ServerPlayer)event.player);
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
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class ModEvents {
        @SubscribeEvent
        public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
            event.register(SuitCapability.class);
        }
    }
}
