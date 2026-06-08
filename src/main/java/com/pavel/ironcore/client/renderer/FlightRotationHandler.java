package com.pavel.ironcore.client.renderer;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;

@Mod.EventBusSubscriber(modid = IronCore.MODID, value = Dist.CLIENT)
public class FlightRotationHandler {
    
    private static float currentTilt = 0.0f;
    private static float currentRoll = 0.0f;
    private static boolean wasRenderedWithTilt = false;
    private static boolean wasFlyingHorizontally = false;
    
    // Переменные для хака с головой
    private static float storedXRot;
    private static float storedXRotO;
    private static boolean modifiedXRot = false;

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        wasRenderedWithTilt = false;
        modifiedXRot = false;

        if (player.getAbilities().flying && !player.onGround()) {
            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (!suit.getSuitTier().equals("none")) {
                    net.minecraft.world.phys.Vec3 velocity = player.getDeltaMovement();
                    double totalSpeed = velocity.length();
                    
                    boolean isFlyingHorizontally = totalSpeed > 0.25;
                    
                    if (isFlyingHorizontally != wasFlyingHorizontally) {
                        wasFlyingHorizontally = isFlyingHorizontally;
                        player.refreshDimensions();
                    }
                    
                    float targetTilt = 0.0f;
                    float targetRoll = 0.0f;
                    
                    if (isFlyingHorizontally) {
                        float pitch = player.getXRot();
                        targetTilt = pitch + 90.0f; 
                        
                        // Рассчитываем дельту поворота (рыскания) для крена
                        float yRotDelta = player.getYRot() - player.yRotO;
                        while (yRotDelta < -180.0f) yRotDelta += 360.0f;
                        while (yRotDelta >= 180.0f) yRotDelta -= 360.0f;
                        
                        // Умножаем дельту на коэффициент, чтобы получить угол крена (ограничиваем до 45 градусов)
                        targetRoll = Mth.clamp(yRotDelta * 4.0f, -45.0f, 45.0f);
                    }
                    
                    currentTilt += (targetTilt - currentTilt) * 0.25f;
                    currentRoll += (targetRoll - currentRoll) * 0.15f;
                    
                    if (currentTilt > 0.5f || Math.abs(currentRoll) > 0.5f) {
                        PoseStack poseStack = event.getPoseStack();
                        poseStack.pushPose();
                        
                        float yBodyRot = Mth.lerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
                        
                        poseStack.translate(0, 0.9, 0);
                        
                        // Применяем вращения: отменяем поворот тела -> крен (Z) -> наклон (X) -> возвращаем поворот
                        poseStack.mulPose(Axis.YP.rotationDegrees(-yBodyRot));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(currentRoll));
                        poseStack.mulPose(Axis.XP.rotationDegrees(currentTilt));
                        poseStack.mulPose(Axis.YP.rotationDegrees(yBodyRot));
                        
                        poseStack.translate(0, -0.9, 0);
                        
                        if (player instanceof AbstractClientPlayer clientPlayer) {
                            clientPlayer.walkAnimation.setSpeed(0.0f);
                            clientPlayer.oBob = 0.0f;
                            clientPlayer.bob = 0.0f;
                        }
                        
                        // ХАК ДЛЯ ГОЛОВЫ:
                        // Майнкрафт вращает голову в зависимости от XRot (pitch) игрока.
                        // Поскольку мы уже повернули всё тело через PoseStack, мы временно обнуляем
                        // pitch сущности перед отрисовкой её модели. Это заставит Майнкрафт думать,
                        // что игрок смотрит прямо, и голова останется ровно вдоль тела (поза торпеды).
                        if (currentTilt > 45.0f) {
                            storedXRot = player.getXRot();
                            storedXRotO = player.xRotO;
                            player.setXRot(0.0f);
                            player.xRotO = 0.0f;
                            modifiedXRot = true;
                        }
                        
                        wasRenderedWithTilt = true;
                    }
                }
            });
        } else {
            if (wasFlyingHorizontally) {
                wasFlyingHorizontally = false;
                player.refreshDimensions();
            }
            currentTilt += (0.0f - currentTilt) * 0.5f;
            if (currentTilt < 0.1f) currentTilt = 0.0f;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        if (modifiedXRot) {
            // Возвращаем настоящий pitch обратно, чтобы не сломать камеру и другие механики
            Player player = event.getEntity();
            player.setXRot(storedXRot);
            player.xRotO = storedXRotO;
            modifiedXRot = false;
        }
        
        if (wasRenderedWithTilt) {
            event.getPoseStack().popPose();
        }
    }
}
