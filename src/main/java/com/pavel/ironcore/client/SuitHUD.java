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

                String energyText = "Energy: " + suit.getEnergy() + " / " + suit.getMaxEnergy() + " FE";
                String tierText = "Suit: " + suit.getSuitTier().toUpperCase();
                String durabilityText = "Armor: " + suit.getFrameDurability() + "%";

                guiGraphics.drawString(minecraft.font, tierText, x, y, 0xFFFFFF);
                guiGraphics.drawString(minecraft.font, energyText, x, y + 10, 0x00AAFF);
                guiGraphics.drawString(minecraft.font, durabilityText, x, y + 20, 0xFFAA00);
                
                int currentY = y + 30;

                if (suit.isFlying()) {
                    guiGraphics.drawString(minecraft.font, "FLIGHT: ACTIVE", x, currentY, 0x00FF00);
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
