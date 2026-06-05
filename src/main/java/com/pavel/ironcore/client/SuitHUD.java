package com.pavel.ironcore.client;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class SuitHUD {
    public static final IGuiOverlay HUD_OVERLAY = (gui, guiGraphics, partialTick, width, height) -> {
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
            
            if (suit.getPalladiumPoisoning() > 0) {
                guiGraphics.drawString(minecraft.font, "POISONING: " + suit.getPalladiumPoisoning(), x, y + 30, 0xFF0000);
            }
        });
    };
}
