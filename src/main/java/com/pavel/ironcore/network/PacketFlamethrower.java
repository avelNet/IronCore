package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

public class PacketFlamethrower {
    public PacketFlamethrower() {}

    public PacketFlamethrower(FriendlyByteBuf buffer) {}

    public void toBytes(FriendlyByteBuf buffer) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (suit.getSuitTier().equals("mk1") && suit.getEnergy() >= 10) {
                    suit.setEnergy(suit.getEnergy() - 10);
                    
                    ServerLevel level = player.serverLevel();
                    Vec3 look = player.getLookAngle();
                    Vec3 pos = player.getEyePosition();

                    // Визуальные частицы убраны для подготовки кастомной анимации
                    // Урон и поджог сущностям в конусе перед игроком
                    Vec3 targetPos = pos.add(look.scale(4.0));
                    AABB area = new AABB(pos, targetPos).inflate(1.5);
                    List<Entity> entities = level.getEntities(player, area, e -> e instanceof LivingEntity);
                    for (Entity entity : entities) {
                        entity.setSecondsOnFire(5);
                        entity.hurt(player.damageSources().inFire(), 3.0f);
                    }
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
