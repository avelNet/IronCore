package com.pavel.ironcore.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pavel.ironcore.IronCore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CoalGeneratorScreen extends AbstractContainerScreen<CoalGeneratorMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IronCore.MODID, "textures/gui/alloy_smelter_gui.png"); // Используем ту же текстуру как заглушку

    public CoalGeneratorScreen(CoalGeneratorMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        if(menu.isBurning()) {
            int k = menu.getScaledBurnTime();
            guiGraphics.blit(TEXTURE, x + 80, y + 54 + 12 - k, 176, 12 - k, 14, k + 1);
        }

        int energyScale = menu.getScaledEnergy();
        guiGraphics.blit(TEXTURE, x + 15, y + 68 - energyScale, 176, 81 - energyScale, 16, energyScale);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if(isHovering(15, 18, 16, 50, mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font, Component.literal(menu.getEnergy() + " / 100000 FE"), mouseX, mouseY);
        }
    }
}
