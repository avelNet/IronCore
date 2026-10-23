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
    public static final String KEY_WEAPON_MODULE = "key.ironcore.weapon_module";

    public static final KeyMapping FLAMETHROWER_KEY = new KeyMapping(KEY_FLAMETHROWER, 
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KEY_CATEGORY_IRONCORE);

    public static final KeyMapping AUTO_BOOST_KEY = new KeyMapping(KEY_AUTO_BOOST,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, KEY_CATEGORY_IRONCORE);

    public static final KeyMapping TOGGLE_MASK_KEY = new KeyMapping(KEY_TOGGLE_MASK,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, KEY_CATEGORY_IRONCORE);

    public static final KeyMapping WEAPON_MODULE_KEY = new KeyMapping(KEY_WEAPON_MODULE,
            KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, KEY_CATEGORY_IRONCORE);

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new KeyBindings());
    }

    /**
     * Apply the launch impulse locally right away. Otherwise the server's "flying=true" can reach
     * us a tick before its velocity packet; vanilla LocalPlayer#aiStep then sees flying while still
     * on the ground, cancels flight and reports that back — takeoff failed about half the time.
     * Mirrors PacketBoostLaunch's checks (minus the server-only cooldown).
     */
    private static void predictLaunch(Minecraft mc) {
        mc.player.getCapability(com.pavel.ironcore.capability.SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
            String tier = suit.getSuitTier();
            if ((tier.equals("mk2") || tier.equals("mk3")) && suit.getEnergy() >= 1000
                    && suit.getIcingLevel() < 100.0f && !suit.isMaskOpen()) {
                com.pavel.ironcore.flight.FlightConfig config = tier.equals("mk3")
                        ? com.pavel.ironcore.flight.FlightConfig.MK3 : com.pavel.ironcore.flight.FlightConfig.MK2;
                mc.player.setDeltaMovement(com.pavel.ironcore.flight.FlightPhysics.computeLaunchVelocity(mc.player.getLookAngle(), config));
            }
        });
    }

    private static long lastSprintTime = 0;
    private static boolean wasSprintDown = false;
    private static long lastFireTime = 0;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean isMk3 = mc.player.getItemBySlot(EquipmentSlot.CHEST).getItem() == com.pavel.ironcore.item.ModItems.MK3_CHESTPLATE.get();
        if (isMk3) {
            // Репульсор — один выстрел на нажатие
            while (FLAMETHROWER_KEY.consumeClick()) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFireTime > 250) {
                    ModMessages.sendToServer(new PacketRepulsorFire());
                    lastFireTime = currentTime;
                }
            }
        } else {
            // Огнемёт — непрерывная струя, пока клавиша зажата
            while (FLAMETHROWER_KEY.consumeClick()) { }
            long currentTime = System.currentTimeMillis();
            if (FLAMETHROWER_KEY.isDown() && currentTime - lastFireTime > 200) {
                ModMessages.sendToServer(new PacketFlamethrower());
                lastFireTime = currentTime;
            }
        }

        // Боевой модуль: G — микроракета, Shift+G — юнибим (перезарядка на сервере)
        while (WEAPON_MODULE_KEY.consumeClick()) {
            ModMessages.sendToServer(new com.pavel.ironcore.network.PacketWeaponModule(mc.player.isShiftKeyDown()));
        }

        while (AUTO_BOOST_KEY.consumeClick()) {
            mc.player.getCapability(com.pavel.ironcore.capability.SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                ModMessages.sendToServer(new PacketSyncAutoBoost(!suit.isAutoBoostEnabled()));
            });
        }

        while (TOGGLE_MASK_KEY.consumeClick()) {
            mc.player.getCapability(com.pavel.ironcore.capability.SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                // Костюма нет — клавиша игнорируется полностью
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
            // Client-side copy for the HUD: the server never syncs this flag back
            mc.player.getCapability(com.pavel.ironcore.capability.SuitCapabilityProvider.SUIT_CAPABILITY)
                    .ifPresent(suit -> suit.setBoostKeyHeld(isSprintDown));

            if (isSprintDown) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSprintTime < 300 && !mc.player.isInWater()) {
                    ModMessages.sendToServer(new PacketBoostLaunch());
                    predictLaunch(mc);
                    com.pavel.ironcore.client.CameraEvents.triggerLaunchKick();
                }
                lastSprintTime = currentTime;
            }
            wasSprintDown = isSprintDown;
        }
    }
}
