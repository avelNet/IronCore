package com.pavel.ironcore.network;

import com.pavel.ironcore.IronCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModMessages {
    private static SimpleChannel INSTANCE;
    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(IronCore.MODID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        net.messageBuilder(PacketSyncSuitData.class, id(), NetworkDirection.PLAY_TO_CLIENT)
                .decoder(PacketSyncSuitData::new)
                .encoder(PacketSyncSuitData::toBytes)
                .consumerMainThread(PacketSyncSuitData::handle)
                .add();

        net.messageBuilder(PacketFlamethrower.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketFlamethrower::new)
                .encoder(PacketFlamethrower::toBytes)
                .consumerMainThread(PacketFlamethrower::handle)
                .add();

        net.messageBuilder(PacketSyncBoostState.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketSyncBoostState::new)
                .encoder(PacketSyncBoostState::toBytes)
                .consumerMainThread(PacketSyncBoostState::handle)
                .add();

        net.messageBuilder(PacketBoostLaunch.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketBoostLaunch::new)
                .encoder(PacketBoostLaunch::toBytes)
                .consumerMainThread(PacketBoostLaunch::handle)
                .add();

        net.messageBuilder(PacketExtractReactor.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(PacketExtractReactor::new)
                .encoder(PacketExtractReactor::toBytes)
                .consumerMainThread(PacketExtractReactor::handle)
                .add();
    }

    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
