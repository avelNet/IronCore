package com.pavel.ironcore.event;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.capability.SuitCapability;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.network.ModMessages;
import com.pavel.ironcore.network.PacketFirstNightChoice;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * «Первая ночь» (DESIGN_V2 §2): в первую ночь у игрока без импланта — засада ослабленных зомби.
 * Смертельный удар оставляет 1 HP, зомби замирают, игрок выбирает «Выжить любой ценой?» (10 с).
 * Принять — адреналин (Сила II, Регенерация, Сопротивление), отказаться или не успеть — смерть.
 * Имплант по-прежнему получают через крафт палладиевого реактора: ночь лишь подводит к нему.
 */
@Mod.EventBusSubscriber(modid = IronCore.MODID)
public final class FirstNightEvents {
    public static final String AMBUSH_TAG = "ironcore_ambush";
    private static final int ZOMBIES = 6;
    private static final int CHOICE_SECONDS = 10;
    private static final long AMBUSH_TIMEOUT = 20 * 60 * 5;
    private static final double AMBUSH_RADIUS = 64.0;

    /** Состояние идущей засады — только на время события, в NBT не сохраняется. */
    private static final class Ambush {
        final long startTime;
        boolean critical;
        boolean adrenalineUsed;
        long deadline;

        Ambush(long startTime) {
            this.startTime = startTime;
        }
    }

    private static final Map<UUID, Ambush> AMBUSHES = new HashMap<>();

    private FirstNightEvents() { }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) return;
        Ambush ambush = AMBUSHES.get(player.getUUID());
        long now = player.level().getGameTime();

        if (ambush == null) {
            if (player.tickCount % 40 == 0) {
                player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                    if (shouldStart(player, suit)) start(player, suit);
                });
            }
            return;
        }

        if (ambush.critical) {
            if (now >= ambush.deadline) resolve(player, false);
            return;
        }
        if (player.tickCount % 20 == 0) {
            boolean anyLeft = !ambushZombies(player).isEmpty();
            if (!anyLeft) {
                AMBUSHES.remove(player.getUUID());
                system(player, "first_night.ironcore.cleared", ChatFormatting.AQUA);
                system(player, "first_night.ironcore.reactor_hint", ChatFormatting.GRAY);
            } else if (now - ambush.startTime > AMBUSH_TIMEOUT) {
                AMBUSHES.remove(player.getUUID());
            }
        }
    }

    private static boolean shouldStart(ServerPlayer player, SuitCapability suit) {
        Level level = player.level();
        long dayTime = level.getDayTime() % 24000;
        return !player.isCreative() && !player.isSpectator()
                && level.dimension() == Level.OVERWORLD
                && level.getDifficulty() != Difficulty.PEACEFUL
                && !suit.isFirstNightTriggered() && !suit.hasEmbeddedReactor()
                && dayTime >= 13000 && dayTime < 22000
                && level.canSeeSky(player.blockPosition());
    }

    /** Запуск засады (из тика или отладочной командой). */
    public static void start(ServerPlayer player, SuitCapability suit) {
        suit.setFirstNightTriggered(true);
        ServerLevel level = player.serverLevel();
        var random = level.getRandom();
        for (int i = 0; i < ZOMBIES; i++) {
            double angle = (Math.PI * 2 * i) / ZOMBIES + random.nextDouble() * 0.6;
            double dist = 9 + random.nextDouble() * 5;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Zombie zombie = EntityType.ZOMBIE.create(level);
            if (zombie == null) continue;
            zombie.moveTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360f, 0);
            // Ослабленные: половина здоровья, без снаряжения, не детёныши
            zombie.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10.0);
            zombie.setHealth(10.0f);
            zombie.setBaby(false);
            zombie.addTag(AMBUSH_TAG);
            zombie.setTarget(player);
            level.addFreshEntity(zombie);
        }
        AMBUSHES.put(player.getUUID(), new Ambush(level.getGameTime()));
        system(player, "first_night.ironcore.ambush", ChatFormatting.RED);
        level.playSound(null, player.blockPosition(), SoundEvents.RAID_HORN.value(), SoundSource.HOSTILE, 0.6f, 1.0f);
    }

    @SubscribeEvent
    public static void onDamage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Ambush ambush = AMBUSHES.get(player.getUUID());
        if (ambush == null) return;
        if (ambush.critical) {
            event.setCanceled(true); // неуязвим, пока решает
            return;
        }
        if (!ambush.adrenalineUsed && player.getHealth() - event.getAmount() <= 1.0f) {
            event.setCanceled(true);
            player.setHealth(1.0f);
            enterCritical(player, ambush);
        }
    }

    private static void enterCritical(ServerPlayer player, Ambush ambush) {
        ambush.critical = true;
        ambush.deadline = player.level().getGameTime() + CHOICE_SECONDS * 20L;
        for (Zombie zombie : ambushZombies(player)) zombie.setNoAi(true); // «застывшее время»
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHOICE_SECONDS * 20 + 20, 255, false, false));
        player.level().playSound(null, player.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS, 1.0f, 1.0f);
        system(player, "first_night.ironcore.critical", ChatFormatting.DARK_RED);
        ModMessages.sendToPlayer(new PacketFirstNightChoice(CHOICE_SECONDS), player);
    }

    /** Ответ игрока (пакет) или истечение таймера. */
    public static void resolve(ServerPlayer player, boolean accept) {
        Ambush ambush = AMBUSHES.get(player.getUUID());
        if (ambush == null || !ambush.critical) return;
        ambush.critical = false;
        ModMessages.sendToPlayer(new PacketFirstNightChoice(0), player); // закрыть экран
        for (Zombie zombie : ambushZombies(player)) zombie.setNoAi(false);
        player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);

        if (accept) {
            ambush.adrenalineUsed = true;
            player.setHealth(10.0f);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 1200, 1));
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 2));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 600, 1));
            player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0f, 0.7f);
            system(player, "first_night.ironcore.accepted", ChatFormatting.AQUA);
        } else {
            AMBUSHES.remove(player.getUUID());
            player.hurt(player.damageSources().genericKill(), Float.MAX_VALUE);
        }
    }

    private static List<Zombie> ambushZombies(ServerPlayer player) {
        return player.level().getEntitiesOfClass(Zombie.class, player.getBoundingBox().inflate(AMBUSH_RADIUS),
                z -> z.isAlive() && z.getTags().contains(AMBUSH_TAG));
    }

    private static void system(ServerPlayer player, String key, ChatFormatting color) {
        player.sendSystemMessage(Component.translatable(key).withStyle(color));
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) AMBUSHES.remove(player.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (AMBUSHES.remove(event.getEntity().getUUID()) != null) {
            for (Zombie zombie : ambushZombies((ServerPlayer) event.getEntity())) zombie.setNoAi(false);
        }
    }

    /** Для отладочной команды: сбросить флаг и запустить засаду прямо сейчас. */
    public static void forceStart(ServerPlayer player) {
        player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
            AMBUSHES.remove(player.getUUID());
            start(player, suit);
        });
    }
}
