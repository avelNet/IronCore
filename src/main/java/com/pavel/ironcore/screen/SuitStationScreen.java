package com.pavel.ironcore.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pavel.ironcore.IronCore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketExtractReactor;
import net.minecraft.client.gui.components.Button;

public class SuitStationScreen extends AbstractContainerScreen<SuitStationMenu> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(IronCore.MODID, "textures/gui/alloy_smelter_gui.png");

    public SuitStationScreen(SuitStationMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;
        
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        
        this.addRenderableWidget(Button.builder(Component.literal("EJECT"), button -> {
            ModMessages.sendToServer(new PacketExtractReactor(menu.blockEntity.getBlockPos()));
        }).bounds(x + 75, y + 53, 38, 16).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
