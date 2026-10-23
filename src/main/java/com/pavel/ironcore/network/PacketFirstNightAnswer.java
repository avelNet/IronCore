package com.pavel.ironcore.network;

import com.pavel.ironcore.event.FirstNightEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Клиент → сервер: ответ на «Выжить любой ценой?». */
public class PacketFirstNightAnswer {
    private final boolean accept;

    public PacketFirstNightAnswer(boolean accept) {
        this.accept = accept;
    }

    public PacketFirstNightAnswer(FriendlyByteBuf buffer) {
        this.accept = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBoolean(accept);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) FirstNightEvents.resolve(player, accept);
        });
        context.setPacketHandled(true);
        return true;
    }
}
