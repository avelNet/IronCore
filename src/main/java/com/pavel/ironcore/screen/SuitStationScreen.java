package com.pavel.ironcore.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.item.module.SuitModules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class SuitStationScreen extends AbstractContainerScreen<SuitStationMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IronCore.MODID, "textures/gui/suit_station_gui.png");
    private static final int TEXT = 0x404040;
    private static final int ACCENT = 0x1E7FA8;

    public SuitStationScreen(SuitStationMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Слоты сверх лимита тира — затемнены (сам слот неактивен и не принимает предметы)
        int slots = SuitModules.slotsFor(menu.getChest());
        for (int i = slots; i < SuitModules.MAX_SLOTS; i++) {
            int sx = x + SuitStationMenu.MODULE_X + i * 18;
            int sy = y + SuitStationMenu.SLOT_Y;
            guiGraphics.fill(sx, sy, sx + 16, sy + 16, 0xC0303030);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, TEXT, false);
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT, false);

        ItemStack chest = menu.getChest();
        int slots = SuitModules.slotsFor(chest);
        Component modules = Component.translatable("gui.ironcore.suit_station.modules",
                SuitModules.installed(chest).size(), slots);
        guiGraphics.drawString(font, modules, SuitStationMenu.MODULE_X - 1, SuitStationMenu.SLOT_Y - 11, ACCENT, false);

        Component hint;
        if (chest.isEmpty()) {
            hint = Component.translatable("gui.ironcore.suit_station.insert");
        } else if (slots == 0) {
            hint = Component.translatable("gui.ironcore.suit_station.no_slots");
        } else {
            hint = null;
        }
        if (hint != null) {
            guiGraphics.drawString(font, hint, 8, SuitStationMenu.SLOT_Y + 24, 0x8B0000, false);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
