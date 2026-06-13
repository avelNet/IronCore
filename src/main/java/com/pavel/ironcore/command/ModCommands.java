package com.pavel.ironcore.command;

import com.pavel.ironcore.capability.SuitCapability;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.network.ModMessages;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.pavel.ironcore.IronCore;

@Mod.EventBusSubscriber(modid = IronCore.MODID)
public class ModCommands {

    public static void acceptCinematic(ServerPlayer player, SuitCapability suit) {
        if (suit.getCinematicStage() != 2) return;

        suit.setCinematicStage(3);

        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2));
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));

        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        player.setHealth(10.0f);
        player.getFoodData().setFoodLevel(10);

        player.serverLevel().getEntities().getAll().forEach(entity -> {
            if (entity instanceof net.minecraft.world.entity.monster.Zombie && entity.distanceTo(player) < 8) {
                entity.setSecondsOnFire(5);
                entity.setDeltaMovement(entity.position().subtract(player.position()).normalize().scale(2.0).add(0, 0.5, 0));
            }
        });

        ModMessages.sendSyncPacket(player);
        player.sendSystemMessage(Component.literal("§b[СИСТЕМА]: Протокол \"ЖЕЛЕЗНОЕ СЕРДЦЕ\" активирован. Требуется состояние глубокого сна для адаптации организма."));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ironcore_accept")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    acceptCinematic(player, suit);
                });
                return 1;
            }));
    }
}
