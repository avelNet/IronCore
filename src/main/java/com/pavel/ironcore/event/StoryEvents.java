package com.pavel.ironcore.event;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
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
                if (!suit.hasEmbeddedReactor() && !suit.isFirstNightTriggered()) {
                    long time = player.level().getDayTime() % 24000;
                    if (time > 13000 && time < 14000) { 
                        triggerAmbush(player, suit);
                    }
                }
            });
        }
    }

    @SubscribeEvent
    public static void onPlayerSleep(PlayerSleepInBedEvent event) {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (!suit.hasEmbeddedReactor() && !suit.isFirstNightTriggered()) {
                    triggerAmbush(player, suit);
                }
            });
        }
    }

    private static void triggerAmbush(ServerPlayer player, com.pavel.ironcore.capability.SuitCapability suit) {
        suit.setFirstNightTriggered(true);
        suit.setCinematicStage(1);
        player.sendSystemMessage(Component.literal("§c[СИСТЕМА]: Зафиксированы множественные биологические угрозы!"));
        
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < 6; i++) {
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie != null) {
                double angle = i * (2 * Math.PI / 6);
                double x = player.getX() + Math.cos(angle) * 7;
                double z = player.getZ() + Math.sin(angle) * 7;
                zombie.moveTo(x, player.getY(), z);
                zombie.finalizeSpawn(level, level.getCurrentDifficultyAt(zombie.blockPosition()), MobSpawnType.EVENT, null, null);
                level.addFreshEntity(zombie);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (suit.getCinematicStage() == 1 && (player.getHealth() - event.getAmount()) <= 1.0f) {
                    event.setAmount(player.getHealth() - 1.01f); 
                    startCinematic(player, suit);
                }
            });
        }
    }

    private static void startCinematic(ServerPlayer player, com.pavel.ironcore.capability.SuitCapability suit) {
        suit.setCinematicStage(2);
        
        ServerLevel level = player.serverLevel();
        level.getEntities().getAll().forEach(entity -> {
            if (entity instanceof Zombie zombie && zombie.distanceTo(player) < 20) {
                zombie.setNoAi(true);
            }
        });
        
        player.sendSystemMessage(Component.literal("§e[СИСТЕМА]: Критическое состояние. Обнаружен поврежденный источник энергии..."));
    }
}
