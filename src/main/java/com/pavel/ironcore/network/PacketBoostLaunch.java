package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketBoostLaunch {
    public PacketBoostLaunch() {}

    public PacketBoostLaunch(FriendlyByteBuf buffer) {}

    public void toBytes(FriendlyByteBuf buffer) {}

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if ((suit.getSuitTier().equals("mk2") || suit.getSuitTier().equals("mk3")) && suit.getEnergy() >= 1000 && suit.getIcingLevel() < 100.0f) {
                    suit.setFlying(true);
                    player.getAbilities().mayfly = true;
                    player.getAbilities().flying = true;
                    player.onUpdateAbilities();
                    
                    Vec3 look = player.getLookAngle();
                    // Переходим от фиксированного "прыжка" к направленному взлету
                    // Множитель скорости зависит от тира костюма
                    double speedMultiplier = suit.getSuitTier().equals("mk3") ? 1.6 : 1.3;
                    
                    // Основной вектор - направление взгляда с ускорением
                    // Добавляем обязательный вертикальный "подхват", чтобы оторваться от земли
                    Vec3 boost = look.scale(speedMultiplier).add(0, 0.6, 0);
                    
                    // Если вектор всё еще слишком пологий (смотрим вниз или прямо), 
                    // принудительно задаем минимальный подъем вверх для "взлета"
                    if (boost.y < 0.7) {
                        boost = new Vec3(boost.x, 0.7, boost.z);
                    }
                    
                    player.setDeltaMovement(boost);
                    player.hasImpulse = true;
                    player.hurtMarked = true;
                    suit.setEnergy(suit.getEnergy() - 100); // 100 FE cost for instant launch
                }
            });
        });
        context.setPacketHandled(true);
        return true;
    }
}
