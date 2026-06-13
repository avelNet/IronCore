package com.pavel.ironcore.event;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.network.ModMessages;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IronCore.MODID)
public class StoryEvents {

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.player;
            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (!suit.hasEmbeddedReactor()) {
                    boolean hasPalladium = false;
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        net.minecraft.world.item.ItemStack stack = player.getInventory().getItem(i);
                        if (!stack.isEmpty()) {
                            if (stack.getItem() == com.pavel.ironcore.item.ModItems.RAW_PALLADIUM.get() || 
                                stack.getItem() == com.pavel.ironcore.block.ModBlocks.PALLADIUM_ORE.get().asItem()) {
                                hasPalladium = true;
                                break;
                            }
                        }
                    }
                    
                    if (hasPalladium && player.tickCount % 100 == 0) {
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.CONFUSION, 200, 0, false, false, true));
                        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 100, 0, false, false, true));
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            net.minecraft.world.item.ItemStack stack = event.getItemStack();
            if (stack.getItem() == com.pavel.ironcore.item.ModItems.PALLADIUM_REACTOR.get()) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    if (!suit.hasEmbeddedReactor() && suit.getCinematicStage() == 0) {
                        stack.shrink(1); // Поглощаем реактор
                        triggerSurgeryShock(player, suit);
                        event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
                        event.setCanceled(true);
                    }
                });
            }
        }
    }

    private static void triggerSurgeryShock(ServerPlayer player, com.pavel.ironcore.capability.SuitCapability suit) {
        suit.setFirstNightTriggered(true);
        suit.setCinematicStage(1);
        
        player.sendSystemMessage(Component.literal("§c[СИСТЕМА]: ВНИМАНИЕ! Выполняется инвазивное вмешательство. Фиксируется болевой шок."));
        
        // Снижаем HP до минимума и убираем голод
        player.setHealth(1.01f);
        player.getFoodData().setFoodLevel(0);
        
        // Накладываем эффекты шока
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS, 200, 0, false, false));
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 200, 255, false, false));

        // Отправляем кликабельное сообщение в чат
        Component message = Component.literal("§7[СИСТЕМА]: Начат процесс интеграции реактора. ")
                .append(Component.literal("§b§l[ИНТЕГРИРОВАТЬ]")
                .withStyle(style -> style.withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/ironcore_accept"))));
        player.sendSystemMessage(message);

        suit.setCinematicStage(2);
        ModMessages.sendSyncPacket(player);
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(net.minecraftforge.event.entity.player.PlayerWakeUpEvent event) {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getEntity();

            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (suit.getCinematicStage() == 3) {
                    suit.setEmbeddedReactor(true);
                    suit.setCinematicStage(0); // Завершено
                    
                    // Даем начальный заряд
                    suit.setMaxEnergy(50000);
                    suit.setEnergy(50000);
                    
                    player.removeEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST);
                    player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, -1, 0, false, false, true));
                    
                    ModMessages.sendSyncPacket(player); 
                    
                    player.sendSystemMessage(Component.literal("§b[СИСТЕМА]: Реактор успешно интегрирован. Костюмы теперь могут получать энергию напрямую."));
                }
            });
        }
    }

    @Mod.EventBusSubscriber(modid = IronCore.MODID, value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static class ClientStoryEvents {
        @SubscribeEvent
        public static void onOpenGui(net.minecraftforge.client.event.ScreenEvent.Opening event) {
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    if (suit.getCinematicStage() == 2 && !(event.getScreen() instanceof net.minecraft.client.gui.screens.ChatScreen)) {
                        event.setCanceled(true); // Блокируем всё кроме чата
                    }
                });
            }
        }
    }
}
