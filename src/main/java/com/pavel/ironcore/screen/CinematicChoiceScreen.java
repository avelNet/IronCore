package com.pavel.ironcore.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketCinematicChoice;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import com.pavel.ironcore.IronCore;

public class CinematicChoiceScreen extends Screen {
    private static final ResourceLocation VIGNETTE = new ResourceLocation(IronCore.MODID, "textures/gui/vignette.png");
    private int timer = 200; // 10 секунд (20 тиков в секунду)

    public CinematicChoiceScreen() {
        super(Component.literal("Cinematic Choice"));
    }

    @Override
    protected void init() {
        int buttonWidth = 120;
        int buttonHeight = 20;
        
        this.addRenderableWidget(Button.builder(Component.literal("§bПРИНЯТЬ"), button -> {
            ModMessages.sendToServer(new PacketCinematicChoice());
            this.onClose();
        }).bounds(this.width / 2 - buttonWidth / 2, this.height / 2 + 30, buttonWidth, buttonHeight).build());
    }

    @Override
    public void tick() {
        if (timer > 0) {
            timer--;
        } else {
            // Если время вышло, закрываем экран (игрок проиграл выбор)
            this.onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Рисуем затемнение и виньетку (эффект блюра по краям)
        this.renderBackground(guiGraphics);
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(0.0f, 0.0f, 0.0f, 0.7f); // Темный оверлей
        guiGraphics.fill(0, 0, this.width, this.height, 0xAA000000);
        
        // 2. Рисуем большой текст
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0f, 2.0f, 2.0f);
        Component title = Component.literal("§cВЫЖИТЬ ЛЮБОЙ ЦЕНОЙ?");
        int titleWidth = this.font.width(title);
        guiGraphics.drawString(this.font, title, (this.width / 4) - (titleWidth / 2), (this.height / 4) - 10, 0xFFFFFF);
        guiGraphics.pose().popPose();

        // 3. Рисуем таймер
        Component timerText = Component.literal("§7Система стабилизации: " + (timer / 20) + "с");
        guiGraphics.drawCenteredString(this.font, timerText, this.width / 2, this.height / 2 + 10, 0xAAAAAA);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Игра не ставится на паузу
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // Нельзя выйти через Esc
    }
}
