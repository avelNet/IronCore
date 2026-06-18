package com.pavel.ironcore.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketFlamethrower;
import com.pavel.ironcore.network.PacketSyncAutoBoost;
import com.pavel.ironcore.network.PacketSyncBoostState;
import com.pavel.ironcore.network.PacketBoostLaunch;
import com.pavel.ironcore.network.PacketRepulsorFire;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final String KEY_CATEGORY_IRONCORE = "key.category.ironcore";
    public static final String KEY_FLAMETHROWER = "key.ironcore.fire_weapon";
    public static final String KEY_AUTO_BOOST = "key.ironcore.toggle_auto_boost";
    public static final String KEY_TOGGLE_MASK = "key.ironcore.toggle_mask";

    public static final KeyMapping flamethrowerKey = new KeyMapping(KEY_FLAMETHROWER, 
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KEY_CATEGORY_IRONCORE);

    public static final KeyMapping autoBoostKey = new KeyMapping(KEY_AUTO_BOOST,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY_IRONCORE);

    public static final KeyMapping toggleMaskKey = new KeyMapping(KEY_TOGGLE_MASK,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY_IRONCORE);

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new KeyBindings());
    }

    private static long lastSprintTime = 0;
    private static boolean wasSprintDown = false;
    private static long lastFireTime = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        while (flamethrowerKey.consumeClick()) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFireTime > 250) { 
                boolean isMk3 = mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == com.pavel.ironcore.item.ModItems.MK3_CHESTPLATE.get();

                if (isMk3) {
                    ModMessages.sendToServer(new PacketRepulsorFire());
                } else {
                    ModMessages.sendToServer(new PacketFlamethrower());
                }
                lastFireTime = currentTime;
            }
        }

        while (autoBoostKey.consumeClick()) {
            mc.player.getCapability(com.pavel.ironcore.capability.SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                ModMessages.sendToServer(new PacketSyncAutoBoost(!suit.isAutoBoostEnabled()));
            });
        }

        while (toggleMaskKey.consumeClick()) {
            mc.player.getCapability(com.pavel.ironcore.capability.SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                // No suit equipped — ignore key entirely
                if (suit.getSuitTier().equals("none")) return;
                if (suit.getSuitTier().equals("mk1")) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cHelmet is fixed on this model."), true);
                    return;
                }
                boolean newState = !suit.isMaskOpen();
                ModMessages.sendToServer(new com.pavel.ironcore.network.PacketSyncMaskState(newState));
                // Client-side instant feedback
                suit.setMaskOpen(newState);
                mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(newState ? "§eHelmet: OPENED" : "§aHelmet: CLOSED"), true);
            });
        }

        boolean isSprintDown = mc.options.keySprint.isDown();
        
        if (isSprintDown != wasSprintDown) {
            ModMessages.sendToServer(new PacketSyncBoostState(isSprintDown));
            
            if (isSprintDown) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSprintTime < 300 && !mc.player.isInWater()) {
                    ModMessages.sendToServer(new PacketBoostLaunch());
                }
                lastSprintTime = currentTime;
            }
            wasSprintDown = isSprintDown;
        }
    }
}
