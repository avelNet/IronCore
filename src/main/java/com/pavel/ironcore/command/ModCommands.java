package com.pavel.ironcore.command;

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

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ironcore_accept")
            .executes(context -> {
                ServerPlayer player = context.getSource().getPlayerOrException();
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    if (suit.getCinematicStage() == 2) {
                        suit.setCinematicStage(3);
                        
                        // Баффы выживания
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
                        
                        player.removeEffect(MobEffects.BLINDNESS);
                        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                        
                        player.setHealth(10.0f);
                        player.getFoodData().setFoodLevel(10);
                        
                        ModMessages.sendSyncPacket(player);
                        
                        player.sendSystemMessage(Component.literal("§b[СИСТЕМА]: Первичная интеграция успешна. Требуется состояние глубокого сна для адаптации организма (наступит ночью)."));
                    }
                });
                return 1;
            }));
    }
}
