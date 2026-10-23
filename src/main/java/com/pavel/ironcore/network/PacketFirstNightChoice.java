package com.pavel.ironcore.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Сервер → клиент: открыть экран «Выжить любой ценой?» на N секунд (0 — закрыть). */
public class PacketFirstNightChoice {
    private final int seconds;

    public PacketFirstNightChoice(int seconds) {
        this.seconds = seconds;
    }

    public PacketFirstNightChoice(FriendlyByteBuf buffer) {
        this.seconds = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeVarInt(seconds);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.pavel.ironcore.client.FirstNightChoiceScreen.handlePacket(seconds)));
        context.setPacketHandled(true);
        return true;
    }
}
