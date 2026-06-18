package com.pavel.ironcore.client;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.pavel.ironcore.IronCore;

@Mod.EventBusSubscriber(modid = IronCore.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SuitHUD {
    private static final ResourceLocation HELMET_OVERLAY = new ResourceLocation(IronCore.MODID, "textures/gui/helmet_overlay.png");

    @SubscribeEvent
    public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("suit_hud", (gui, guiGraphics, partialTick, width, height) -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || minecraft.options.hideGui) return;

            minecraft.player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (suit.getSuitTier().equals("none")) {
                    if (suit.getPalladiumPoisoning() > 0) {
                        renderToxicityOnly(guiGraphics, minecraft, suit, width, height);
                    }
                    return;
                }

                // If helmet is open, don't render tech HUD or overlay
                if (suit.isMaskOpen()) return;

                // Render Helmet Mask (if in first person)
                if (minecraft.options.getCameraType().isFirstPerson()) {
                    renderHelmetOverlay(guiGraphics, width, height);
                }

                renderTechHUD(guiGraphics, minecraft, suit, width, height);
            });
        });
    }

    private static void renderHelmetOverlay(GuiGraphics guiGraphics, int width, int height) {
        // Draw a semi-transparent black overlay with "helmet vision" feel
        // In a real mod, we'd use a texture, but we can draw some bars to simulate it
        int color = 0x44000000; // Very transparent black
        guiGraphics.fill(0, 0, width, height / 8, color); // Top bar
        guiGraphics.fill(0, height - height / 8, width, height, color); // Bottom bar
        
        // Corner "brackets"
        int bracketColor = 0xAA00AAFF; // Cyan tech color
        int len = 20;
        int thick = 1;
        
        // Top Left
        guiGraphics.fill(10, 10, 10 + len, 10 + thick, bracketColor);
        guiGraphics.fill(10, 10, 10 + thick, 10 + len, bracketColor);
        
        // Top Right
        guiGraphics.fill(width - 10 - len, 10, width - 10, 10 + thick, bracketColor);
        guiGraphics.fill(width - 10 - thick, 10, width - 10, 10 + len, bracketColor);
        
        // Bottom Left
        guiGraphics.fill(10, height - 10 - thick, 10 + len, height - 10, bracketColor);
        guiGraphics.fill(10, height - 10 - len, 10 + thick, height - 10, bracketColor);
        
        // Bottom Right
        guiGraphics.fill(width - 10 - len, height - 10 - thick, width - 10, height - 10, bracketColor);
        guiGraphics.fill(width - 10 - thick, height - 10 - len, width - 10, height - 10, bracketColor);
    }

    private static void renderTechHUD(GuiGraphics guiGraphics, Minecraft mc, com.pavel.ironcore.capability.SuitCapability suit, int width, int height) {
        int cyan = 0x00AAFF;
        int orange = 0xFFAA00;
        int red = 0xFF0000;

        // --- LEFT SIDE: SUIT STATUS ---
        int lx = 20;
        int ly = height / 2 - 40;

        guiGraphics.drawString(mc.font, "STARK OS v.1.0", lx, ly - 15, 0x55FFFF);
        guiGraphics.drawString(mc.font, "MARK " + suit.getSuitTier().toUpperCase().replace("MK", ""), lx, ly, 0xFFFFFF);
        
        // Energy Bar
        float energyPercent = suit.getMaxEnergy() > 0 ? (float) suit.getEnergy() / suit.getMaxEnergy() : 0;
        int energyBarColor = (energyPercent < 0.1) ? ((mc.player.tickCount % 20 < 10) ? red : orange) : cyan;
        
        guiGraphics.fill(lx, ly + 12, lx + 100, ly + 14, 0x44FFFFFF); // Background
        guiGraphics.fill(lx, ly + 12, lx + (int)(100 * energyPercent), ly + 14, 0xFF00AAFF); // Foreground
        guiGraphics.drawString(mc.font, "PWR: " + (int)(energyPercent * 100) + "%", lx, ly + 16, energyBarColor);

        // Armor Integrity
        guiGraphics.drawString(mc.font, "ARM: " + suit.getFrameDurability() + "%", lx, ly + 30, (suit.getFrameDurability() < 30) ? red : orange);

        // --- RIGHT SIDE: FLIGHT DATA ---
        int rx = width - 100;
        int ry = height / 2 - 40;

        // На земле getDeltaMovement().y ≈ -0.078 (гравитация) → 5.6 км/ч шума; обнуляем.
        double speed = mc.player.onGround() ? 0.0 : mc.player.getDeltaMovement().length() * 20.0 * 3.6;
        guiGraphics.drawString(mc.font, "SPD: " + String.format("%.1f", speed), rx, ry, 0x00FF00);
        guiGraphics.drawString(mc.font, "ALT: " + (int)mc.player.getY(), rx, ry + 10, 0x00FF00);

        // abilities.flying — точный ванильный флаг активного полёта; suit.isFlying() только
        // от PacketBoostLaunch и не сбрасывается при обычной посадке.
        if (mc.player.getAbilities().flying) {
            String flightMode = suit.isTurbo() ? "§b§lTURBO" : (suit.isBoostKeyHeld() ? "§eBOOST" : "§aCRUISE");
            guiGraphics.drawString(mc.font, "MODE: " + flightMode, rx, ry + 25, 0xFFFFFF);
        }

        // Warning widgets
        int warningY = height - 60;
        if (suit.getIcingLevel() > 20) {
            guiGraphics.drawString(mc.font, "§bICE WARNING: " + (int)suit.getIcingLevel() + "%", width / 2 - 50, warningY, 0xFFFFFF);
            warningY -= 12;
        }
        if (suit.getHeat() > 60) {
            guiGraphics.drawString(mc.font, "§6HEAT WARNING: " + (int)suit.getHeat() + "%", width / 2 - 50, warningY, 0xFFFFFF);
            warningY -= 12;
        }
        if (suit.getPalladiumPoisoning() > 10) {
            guiGraphics.drawString(mc.font, "§dTOXICITY: " + (int)suit.getPalladiumPoisoning() + "%", width / 2 - 50, warningY, 0xFFFFFF);
        }
    }

    private static void renderToxicityOnly(GuiGraphics guiGraphics, Minecraft mc, com.pavel.ironcore.capability.SuitCapability suit, int width, int height) {
        guiGraphics.drawString(mc.font, "§dTOXICITY: " + String.format("%.1f%%", suit.getPalladiumPoisoning()), 10, 10, 0xFFFFFF);
    }
}
