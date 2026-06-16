package com.pavel.ironcore.client;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = IronCore.MODID, value = Dist.CLIENT)
public class CameraEvents {
    private static float currentRoll = 0.0f;
    private static float currentFovModifier = 1.0f;
    private static int launchKickTicks = 0;

    public static void triggerLaunchKick() {
        launchKickTicks = 8;
    }

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                // СИИНЕМАТИК: Стадия 2 (падение на колено)
                if (suit.getCinematicStage() == 2) {
                    if (mc.options.getCameraType() != net.minecraft.client.CameraType.THIRD_PERSON_BACK) {
                        mc.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
                    }
                    
                    // Позиционирование: Снизу-слева, чуть сзади
                    // Вращаем камеру так, чтобы она смотрела на игрока под углом
                    event.setPitch(-15.0f); // Смотрим чуть вверх
                    event.setYaw(mc.player.getYRot() + 150.0f); // Ракурс сбоку-сзади
                    return;
                }

                // Наклон камеры теперь срабатывает ТОЛЬКО в режиме ТУРБО
                if (suit.isFlying() && mc.player.getAbilities().flying && suit.isTurbo()) {
                    // Используем дельту поворота ТЕЛА (yBodyRot) вместо головы для синхронизации с моделью
                    float yRotDelta = mc.player.yBodyRot - mc.player.yBodyRotO;
                    while (yRotDelta < -180.0f) yRotDelta += 360.0f;
                    while (yRotDelta >= 180.0f) yRotDelta -= 360.0f;
                    
                    // Ограничиваем до 30 градусов
                    float targetRoll = Mth.clamp(yRotDelta * 5.0f, -30.0f, 30.0f);
                    
                    // Возвращаю более отзывчивую интерполяцию для Турбо (0.1), так как на такой скорости важна динамика
                    currentRoll += (targetRoll - currentRoll) * 0.1f;
                } else {
                    // Плавно возвращаем камеру в горизонт, если вышли из Турбо
                    currentRoll += (0.0f - currentRoll) * 0.1f;
                }

                // Apply roll if it's significant enough
                if (Math.abs(currentRoll) > 0.1f) {
                    event.setRoll(currentRoll);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onComputeFov(ViewportEvent.ComputeFov event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                float targetFov = 1.0f;
                if (suit.isTurbo() && suit.isFlying() && mc.player.getAbilities().flying) {
                    targetFov = 1.20f; // +20% FOV
                }
                if (launchKickTicks > 0) {
                    targetFov += 0.15f * (launchKickTicks / 8.0f);
                    launchKickTicks--;
                }

                // Smooth interpolation
                currentFovModifier += (targetFov - currentFovModifier) * 0.05f;
                
                if (currentFovModifier > 1.001f || currentFovModifier < 0.999f) {
                    event.setFOV(event.getFOV() * currentFovModifier);
                }
            });
        }
    }
}
