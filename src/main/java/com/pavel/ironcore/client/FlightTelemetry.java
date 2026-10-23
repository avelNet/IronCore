package com.pavel.ironcore.client;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Debug tool: /ironcore_telemetry start|stop writes one CSV row per client tick
 * (position, velocity, suit state) to logs/ironcore-telemetry.csv.
 * Client-side because flight physics is client-authoritative.
 */
@Mod.EventBusSubscriber(modid = IronCore.MODID, value = Dist.CLIENT)
public final class FlightTelemetry {
    private static PrintWriter writer;
    private static long tick;

    private FlightTelemetry() { }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ironcore_telemetry")
                .then(Commands.argument("action", StringArgumentType.word())
                        .executes(ctx -> {
                            String action = StringArgumentType.getString(ctx, "action");
                            if ("start".equals(action)) {
                                start();
                            } else {
                                stop();
                            }
                            return 1;
                        })));
    }

    private static void start() {
        stop();
        Path file = Minecraft.getInstance().gameDirectory.toPath().resolve("logs").resolve("ironcore-telemetry.csv");
        try {
            Files.createDirectories(file.getParent());
            writer = new PrintWriter(Files.newBufferedWriter(file));
            writer.println("tick,x,y,z,vx,vy,vz,speed_kmh,on_ground,flying,sprint_key,tier,energy,max_energy,"
                    + "heat,icing,poison,turbo,flight_timer,mask_open");
            tick = 0;
            message("§a[Telemetry] recording → " + file);
        } catch (IOException e) {
            message("§c[Telemetry] " + e.getMessage());
        }
    }

    private static void stop() {
        if (writer != null) {
            writer.close();
            writer = null;
            message("§e[Telemetry] stopped after " + tick + " ticks");
        }
    }

    private static void message(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) player.displayClientMessage(Component.literal(text), false);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || writer == null) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            stop();
            return;
        }
        Vec3 pos = player.position();
        Vec3 vel = player.getDeltaMovement();
        player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> writer.printf(
                java.util.Locale.ROOT,
                "%d,%.3f,%.3f,%.3f,%.4f,%.4f,%.4f,%.2f,%b,%b,%b,%s,%d,%d,%.2f,%.2f,%.3f,%b,%d,%b%n",
                tick, pos.x, pos.y, pos.z, vel.x, vel.y, vel.z, vel.length() * 20.0 * 3.6,
                player.onGround(), player.getAbilities().flying, mc.options.keySprint.isDown(),
                suit.getSuitTier(), suit.getEnergy(), suit.getMaxEnergy(), suit.getHeat(),
                suit.getIcingLevel(), suit.getPalladiumPoisoning(), suit.isTurbo(), suit.getFlightTimer(),
                suit.isMaskOpen()));
        tick++;
    }
}
