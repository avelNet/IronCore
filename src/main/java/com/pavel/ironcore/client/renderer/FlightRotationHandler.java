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

import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = IronCore.MODID, value = Dist.CLIENT)
public class FlightRotationHandler {
    
    private static class PlayerRotationData {
        float currentTilt = 0.0f;
        float currentRoll = 0.0f;
        boolean wasFlyingHorizontally = false;
        
        // Хак для головы
        float storedXRot;
        float storedXRotO;
        boolean modifiedXRot = false;
        boolean wasRenderedWithTilt = false;
    }

    private static final WeakHashMap<Player, PlayerRotationData> PLAYER_ROTATION_MAP = new WeakHashMap<>();

    @SubscribeEvent
    public static void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PlayerRotationData data = PLAYER_ROTATION_MAP.computeIfAbsent(player, k -> new PlayerRotationData());
        data.wasRenderedWithTilt = false;
        data.modifiedXRot = false;

        if (player.getAbilities().flying && !player.onGround()) {
            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (!suit.getSuitTier().equals("none")) {
                    boolean isFlyingHorizontally = false;
                    
                    // Локальное предсказание для самого игрока (убирает дергание)
                    if (player == net.minecraft.client.Minecraft.getInstance().player) {
                        boolean isBoosting = net.minecraft.client.Minecraft.getInstance().options.keySprint.isDown();
                        if (suit.isMaskOpen()) isBoosting = false; // Учитываем маску
                        isFlyingHorizontally = isBoosting && player.getDeltaMovement().length() > 0.1;
                    } else {
                        // Для остальных игроков используем синхронизированные данные с сервера
                        isFlyingHorizontally = suit.wasFlyingHorizontally();
                    }
                    
                    float targetTilt = 0.0f;
                    float targetRoll = 0.0f;
                    
                    if (isFlyingHorizontally) {
                        float pitch = player.getXRot();
                        targetTilt = pitch + 90.0f; 
                        
                        float yRotDelta = player.yBodyRot - player.yBodyRotO;
                        while (yRotDelta < -180.0f) yRotDelta += 360.0f;
                        while (yRotDelta >= 180.0f) yRotDelta -= 360.0f;
                        
                        targetRoll = Mth.clamp(yRotDelta * 5.0f, -30.0f, 30.0f);
                    }

                    data.currentTilt += (targetTilt - data.currentTilt) * 0.25f;
                    data.currentRoll += (targetRoll - data.currentRoll) * 0.25f;
                    
                    if (data.currentTilt > 0.5f || Math.abs(data.currentRoll) > 0.5f) {
                        PoseStack poseStack = event.getPoseStack();
                        poseStack.pushPose();
                        
                        float yBodyRot = Mth.lerp(event.getPartialTick(), player.yBodyRotO, player.yBodyRot);
                        
                        poseStack.translate(0, 0.9, 0);
                        
                        poseStack.mulPose(Axis.YP.rotationDegrees(-yBodyRot));
                        poseStack.mulPose(Axis.ZP.rotationDegrees(data.currentRoll));
                        poseStack.mulPose(Axis.XP.rotationDegrees(data.currentTilt));
                        poseStack.mulPose(Axis.YP.rotationDegrees(yBodyRot));
                        
                        poseStack.translate(0, -0.9, 0);
                        
                        if (player instanceof AbstractClientPlayer clientPlayer) {
                            clientPlayer.walkAnimation.setSpeed(0.0f);
                            clientPlayer.oBob = 0.0f;
                            clientPlayer.bob = 0.0f;
                        }
                        
                        if (data.currentTilt > 45.0f) {
                            data.storedXRot = player.getXRot();
                            data.storedXRotO = player.xRotO;
                            player.setXRot(0.0f);
                            player.xRotO = 0.0f;
                            data.modifiedXRot = true;
                        }
                        
                        data.wasRenderedWithTilt = true;
                    }
                }
            });
        } else {
            data.currentTilt += (0.0f - data.currentTilt) * 0.5f;
            if (data.currentTilt < 0.1f) data.currentTilt = 0.0f;
        }
    }

    @SubscribeEvent
    public static void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        PlayerRotationData data = PLAYER_ROTATION_MAP.get(event.getEntity());
        if (data == null) return;

        if (data.modifiedXRot) {
            Player player = event.getEntity();
            player.setXRot(data.storedXRot);
            player.xRotO = data.storedXRotO;
            data.modifiedXRot = false;
        }
        
        if (data.wasRenderedWithTilt) {
            event.getPoseStack().popPose();
        }
    }
}
