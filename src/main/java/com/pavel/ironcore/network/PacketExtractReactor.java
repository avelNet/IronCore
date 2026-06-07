package com.pavel.ironcore.network;

import com.pavel.ironcore.block.entity.SuitStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PacketExtractReactor {
    private final BlockPos pos;

    public PacketExtractReactor(BlockPos pos) {
        this.pos = pos;
    }

    public PacketExtractReactor(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            Level level = player.level();
            if (level.isLoaded(pos)) {
                BlockEntity entity = level.getBlockEntity(pos);
                if (entity instanceof SuitStationBlockEntity station) {
                    station.extractReactor();
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
