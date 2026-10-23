package com.pavel.ironcore.network;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.item.module.WeaponModules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Выстрел боевого модуля: secondary (Shift+G) — юнибим, иначе микроракета. */
public class PacketWeaponModule {
    private final boolean secondary;

    public PacketWeaponModule(boolean secondary) {
        this.secondary = secondary;
    }

    public PacketWeaponModule(FriendlyByteBuf buffer) {
        this.secondary = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(secondary);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY)
                    .ifPresent(suit -> WeaponModules.fire(player, suit, secondary));
        });
        context.setPacketHandled(true);
        return true;
    }
}
