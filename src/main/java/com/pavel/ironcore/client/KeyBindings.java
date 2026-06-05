package com.pavel.ironcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketFlamethrower;
import com.pavel.ironcore.network.PacketToggleFlight;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String KEY_CATEGORY_IRONCORE = "key.category.ironcore";
    public static final String KEY_FLAMETHROWER = "key.ironcore.flamethrower";
    public static final String KEY_TOGGLE_FLIGHT = "key.ironcore.toggle_flight";

    public static final KeyMapping flamethrowerKey = new KeyMapping(KEY_FLAMETHROWER, 
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KEY_CATEGORY_IRONCORE);
    public static final KeyMapping toggleFlightKey = new KeyMapping(KEY_TOGGLE_FLIGHT, 
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_ALT, KEY_CATEGORY_IRONCORE);

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new KeyBindings());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.Key event) {
        if (flamethrowerKey.isDown()) {
            ModMessages.sendToServer(new PacketFlamethrower());
        }
        
        if (event.getKey() == toggleFlightKey.getKey().getValue() && event.getAction() == GLFW.GLFW_PRESS) {
            ModMessages.sendToServer(new PacketToggleFlight());
        }
    }
}
