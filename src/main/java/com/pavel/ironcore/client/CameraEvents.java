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

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                // If flying and sprinting (boosting), apply camera roll
                if (suit.isFlying() && mc.player.getAbilities().flying && mc.player.isSprinting()) {
                    // Calculate yaw delta (how fast the player is turning their head horizontally)
                    float yawDelta = mc.player.getYRot() - mc.player.yRotO;
                    
                    // Target roll based on turn speed (clamp to max 30 degrees tilt)
                    float targetRoll = Mth.clamp(yawDelta * 1.5f, -30.0f, 30.0f);
                    
                    // Smoothly interpolate roll (frame-independent smoothing)
                    currentRoll += (targetRoll - currentRoll) * 0.1f;
                } else {
                    // Smoothly return to 0 when not boosting
                    currentRoll += (0.0f - currentRoll) * 0.1f;
                }

                // Apply roll if it's significant enough
                if (Math.abs(currentRoll) > 0.1f) {
                    event.setRoll(currentRoll);
                }
            });
        }
    }
}
