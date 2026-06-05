package com.pavel.ironcore.client;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.pavel.ironcore.IronCore;
import net.minecraftforge.api.distmarker.Dist;

@Mod.EventBusSubscriber(modid = IronCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SuitHUD {
    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("suit_hud", (gui, guiGraphics, partialTick, width, height) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null) return;

            minecraft.player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (suit.getSuitTier().equals("none")) return;

                int x = 10;
                int y = 10;

                int energyColor = 0x00AAFF; // Default blue
                if (suit.getEnergy() <= 1000) {
                    energyColor = (minecraft.player.tickCount % 20 < 10) ? 0xFF0000 : 0xFFFF00; // Blink Red/Yellow
                }

                String energyText = "Energy: " + suit.getEnergy() + " / " + suit.getMaxEnergy() + " FE";
                String tierText = "Suit: " + suit.getSuitTier().toUpperCase();
                String durabilityText = "Armor: " + suit.getFrameDurability() + "%";
                
                // Calculate speed in km/h using absolute position delta
                net.minecraft.world.phys.Vec3 pos = minecraft.player.position();
                net.minecraft.world.phys.Vec3 prevPos = new net.minecraft.world.phys.Vec3(minecraft.player.xo, minecraft.player.yo, minecraft.player.zo);
                double distMoved = pos.distanceTo(prevPos);
                double speedKmH = distMoved * 20.0 * 3.6;
                
                String speedText = "SPEED: " + String.format("%.1f", speedKmH) + " km/h";

                guiGraphics.drawString(minecraft.font, tierText, x, y, 0xFFFFFF);
                guiGraphics.drawString(minecraft.font, energyText, x, y + 10, energyColor);
                guiGraphics.drawString(minecraft.font, durabilityText, x, y + 20, 0xFFAA00);
                guiGraphics.drawString(minecraft.font, speedText, x, y + 30, 0x00FF00); // Green speed
                
                int currentY = y + 40;

                if (suit.isFlying()) {
                    if (minecraft.player.getAbilities().flying) {
                        guiGraphics.drawString(minecraft.font, "FLIGHT: ACTIVE", x, currentY, 0x00FF00); // Green
                    } else {
                        guiGraphics.drawString(minecraft.font, "FLIGHT: STANDBY", x, currentY, 0xFFFF00); // Yellow
                    }
                    currentY += 10;
                }

                if (suit.getIcingLevel() > 0) {
                    int color = 0x00FFFF; // Light blue
                    if (suit.getIcingLevel() > 50) color = 0xFFA500; // Orange
                    if (suit.getIcingLevel() > 80) color = 0xFF0000; // Red
                    guiGraphics.drawString(minecraft.font, "ICING: " + String.format("%.1f%%", suit.getIcingLevel()), x, currentY, color);
                    currentY += 10;
                }

                if (suit.getPalladiumPoisoning() > 0) {
                    guiGraphics.drawString(minecraft.font, "POISONING: " + suit.getPalladiumPoisoning(), x, currentY, 0xFF0000);
                }
            });
        });
    }
}
