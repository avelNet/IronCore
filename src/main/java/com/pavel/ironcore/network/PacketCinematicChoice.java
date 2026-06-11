package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketCinematicChoice {
    public PacketCinematicChoice() {
    }

    public PacketCinematicChoice(FriendlyByteBuf buf) {
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    if (suit.getCinematicStage() == 2) {
                        suit.setCinematicStage(3); // Переходим к стадии триумфа
                        
                        // Баффы из диздока: Сила II и Регенерация
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 600, 1)); 
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 2));
                        
                        // Эффект "вспышки": отбрасываем и поджигаем зомби вокруг
                        player.serverLevel().getEntities().getAll().forEach(entity -> {
                            if (entity instanceof net.minecraft.world.entity.monster.Zombie && entity.distanceTo(player) < 8) {
                                entity.setSecondsOnFire(5);
                                entity.setDeltaMovement(entity.position().subtract(player.position()).normalize().scale(2.0).add(0, 0.5, 0));
                            }
                        });
                        
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§b[СИСТЕМА]: Протокол \"ЖЕЛЕЗНОЕ СЕРДЦЕ\" активирован."));
                    }
                });
            }
        });
        return true;
    }
}
