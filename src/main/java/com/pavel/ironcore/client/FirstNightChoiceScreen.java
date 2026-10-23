package com.pavel.ironcore.client;

import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketFirstNightAnswer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * «Выжить любой ценой?» — выбор в критический момент Первой ночи. Игра не на паузе:
 * зомби вокруг застыли, камера смотрит снизу (см. {@link CameraEvents}).
 */
public class FirstNightChoiceScreen extends Screen {
    private static boolean active;
    private int ticksLeft;
    private boolean answered;
    private net.minecraft.client.CameraType previousCamera;

    private FirstNightChoiceScreen(int seconds) {
        super(Component.translatable("first_night.ironcore.choice_title"));
        this.ticksLeft = seconds * 20;
    }

    /** Экран открыт — камера в кинематографичном ракурсе. */
    public static boolean isActive() {
        return active;
    }

    public static void handlePacket(int seconds) {
        Minecraft mc = Minecraft.getInstance();
        if (seconds > 0) {
            mc.setScreen(new FirstNightChoiceScreen(seconds));
        } else if (mc.screen instanceof FirstNightChoiceScreen) {
            mc.setScreen(null);
        }
    }

    @Override
    protected void init() {
        if (!active) previousCamera = Minecraft.getInstance().options.getCameraType();
        active = true;
        int cx = width / 2;
        int y = height / 2 + 20;
        addRenderableWidget(Button.builder(Component.translatable("first_night.ironcore.accept"), b -> answer(true))
                .bounds(cx - 104, y, 100, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("first_night.ironcore.decline"), b -> answer(false))
                .bounds(cx + 4, y, 100, 20).build());
    }

    private void answer(boolean accept) {
        if (answered) return;
        answered = true;
        ModMessages.sendToServer(new PacketFirstNightAnswer(accept));
        onClose();
    }

    @Override
    public void tick() {
        if (--ticksLeft <= 0) onClose(); // сервер сам засчитает таймаут
    }

    @Override
    public void removed() {
        active = false;
        if (previousCamera != null) Minecraft.getInstance().options.setCameraType(previousCamera);
        super.removed();
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Пульсирующее красное затемнение по краям — «потеря сознания»
        float pulse = 0.5f + 0.5f * (float) Math.sin((ticksLeft + partialTick) * 0.25);
        int edge = ((int) (0x60 + 0x50 * pulse) << 24) | 0x300000;
        g.fillGradient(0, 0, width, height / 3, edge, 0x00000000);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00000000, edge);
        g.fill(0, height / 2 - 40, width, height / 2 + 50, 0x90000000);

        g.drawCenteredString(font, title.copy().withStyle(s -> s.withBold(true)), width / 2, height / 2 - 30, 0xFF5555);
        g.drawCenteredString(font, Component.translatable("first_night.ironcore.choice_subtitle"), width / 2, height / 2 - 14, 0xAAAAAA);
        int seconds = Math.max(0, (ticksLeft + 19) / 20);
        g.drawCenteredString(font, Component.translatable("first_night.ironcore.choice_timer", seconds), width / 2, height / 2 + 2, 0xFFAA00);
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
