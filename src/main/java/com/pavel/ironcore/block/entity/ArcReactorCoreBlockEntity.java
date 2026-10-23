package com.pavel.ironcore.block.entity;

import com.pavel.ironcore.capability.SuitCapability;
import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import com.pavel.ironcore.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

import java.util.List;

public class ArcReactorCoreBlockEntity extends AbstractEnergyMachineBlockEntity {
    public static final double RANGE = 16.0;
    private static final int TICK_INTERVAL = 20;
    private static final int RECHARGE_PER_INTERVAL = 200;
    private static final long GREETING_GAP = 100; // тиков вне зоны, после которых J.A.R.V.I.S. снова приветствует

    private final ItemStackHandler itemHandler = new ItemStackHandler(0);
    private final NotifyingEnergyStorage energyStorage = new NotifyingEnergyStorage(500000, 2000, 0, this::setChanged);

    public ArcReactorCoreBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ARC_REACTOR_CORE_BE.get(), pos, state);
    }

    @Override
    protected ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        energyStorage.setEnergyDirect(tag.getInt("energy"));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ArcReactorCoreBlockEntity entity) {
        if (level.isClientSide()) return;
        if (level.getGameTime() % TICK_INTERVAL != 0) return;

        AABB zone = AABB.ofSize(pos.getCenter(), RANGE * 2, RANGE * 2, RANGE * 2);

        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, zone,
                p -> p.distanceToSqr(pos.getCenter()) <= RANGE * RANGE);
        for (ServerPlayer player : players) {
            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                // In Creative mode skip FE cost so the block is testable without a generator
                boolean blockCanCharge = player.isCreative() || entity.energyStorage.getEnergyStored() > 0;

                if (blockCanCharge) {
                    greet(player, suit, level.getGameTime());
                }

                if (blockCanCharge && suit.hasEmbeddedReactor() && suit.getEnergy() < suit.getMaxEnergy()) {
                    int toGive = Math.min(RECHARGE_PER_INTERVAL, suit.getMaxEnergy() - suit.getEnergy());
                    suit.setEnergy(suit.getEnergy() + toGive);
                    if (!player.isCreative()) entity.energyStorage.extractEnergy(toGive, false);
                    ModMessages.sendSyncPacket(player);
                }
            });
        }

        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class, zone,
                m -> m.distanceToSqr(pos.getCenter()) <= RANGE * RANGE);
        for (Monster monster : hostiles) {
            monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, TICK_INTERVAL + 10, 0, false, false, false));
        }
    }

    public boolean isPowered() {
        return energyStorage.getEnergyStored() > 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null) StarkNetwork.register(level, worldPosition);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (level != null) StarkNetwork.unregister(level, worldPosition);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        if (level != null) StarkNetwork.unregister(level, worldPosition);
    }

    /**
     * J.A.R.V.I.S.: приветствие при входе в Stark Network (не чаще, чем после 5 с вне зоны —
     * поэтому несколько ядер рядом не дублируют сообщение). Первый контакт — отдельная фраза.
     */
    private static void greet(ServerPlayer player, SuitCapability suit, long gameTime) {
        boolean returning = gameTime - suit.getLastNetworkTick() > GREETING_GAP;
        suit.setLastNetworkTick(gameTime);
        if (!returning) return;

        if (!suit.isJarvisUnlocked()) {
            suit.setJarvisUnlocked(true);
            player.sendSystemMessage(jarvis("jarvis.ironcore.first_contact"));
        } else {
            player.sendSystemMessage(jarvis("jarvis.ironcore.welcome_back"));
        }

        if (suit.hasEmbeddedReactor()) {
            int implant = suit.getMaxEnergy() > 0 ? suit.getEnergy() * 100 / suit.getMaxEnergy() : 0;
            String tier = suit.getSuitTier().equals("none") ? "—" : suit.getSuitTier().toUpperCase().replace("MK", "Mk");
            player.sendSystemMessage(Component.translatable("jarvis.ironcore.status", implant,
                    String.format(java.util.Locale.ROOT, "%.1f", suit.getPalladiumPoisoning()), tier)
                    .withStyle(ChatFormatting.GRAY));
            if (suit.getPalladiumPoisoning() >= 50.0f) {
                player.sendSystemMessage(jarvis("jarvis.ironcore.toxicity_warning").withStyle(ChatFormatting.LIGHT_PURPLE));
            }
            if (implant < 20) {
                player.sendSystemMessage(jarvis("jarvis.ironcore.low_energy").withStyle(ChatFormatting.GOLD));
            }
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.4f, 1.6f);
    }

    private static MutableComponent jarvis(String key) {
        return Component.literal("[J.A.R.V.I.S.]: ").withStyle(ChatFormatting.AQUA)
                .append(Component.translatable(key).withStyle(ChatFormatting.WHITE));
    }
}
