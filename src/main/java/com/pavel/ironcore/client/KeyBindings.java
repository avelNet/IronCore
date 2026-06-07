package com.pavel.ironcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketFlamethrower;
import com.pavel.ironcore.network.PacketSyncBoostState;
import com.pavel.ironcore.network.PacketBoostLaunch;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String KEY_CATEGORY_IRONCORE = "key.category.ironcore";
    public static final String KEY_FLAMETHROWER = "key.ironcore.flamethrower";

    public static final KeyMapping flamethrowerKey = new KeyMapping(KEY_FLAMETHROWER, 
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KEY_CATEGORY_IRONCORE);

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new KeyBindings());
    }

    private static long lastSprintTime = 0;
    private static boolean wasSprintDown = false;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        while (flamethrowerKey.consumeClick()) {
            ModMessages.sendToServer(new PacketFlamethrower());
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            boolean isSprintDown = mc.options.keySprint.isDown();
            
            if (isSprintDown != wasSprintDown) {
                ModMessages.sendToServer(new PacketSyncBoostState(isSprintDown));
                
                if (isSprintDown) {
                    long currentTime = System.currentTimeMillis();
                    // Double tap detect
                    if (currentTime - lastSprintTime < 300) {
                        // Launch ONLY if on ground
                        if (mc.player.onGround()) {
                            ModMessages.sendToServer(new PacketBoostLaunch());
                        }
                    }
                    lastSprintTime = currentTime;
                }
                wasSprintDown = isSprintDown;
            }
        }
    }
}
