package com.pavel.ironcore.block.entity;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import com.pavel.ironcore.network.ModMessages;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
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
                // Unlock JARVIS on first contact regardless of reactor type
                if (!suit.isJarvisUnlocked()) {
                    suit.setJarvisUnlocked(true);
                }

                // In Creative mode skip FE cost so the block is testable without a generator
                boolean blockCanCharge = player.isCreative() || entity.energyStorage.getEnergyStored() > 0;

                if (blockCanCharge && suit.getEnergy() < suit.getMaxEnergy()) {
                    if (suit.hasEmbeddedReactor()) {
                        // Embedded reactor: energy lives purely in SuitCapability —
                        // BaseSuitItem no longer overwrites it to max each tick,
                        // so suit.setEnergy() sticks until next flight drain.
                        int toGive = Math.min(RECHARGE_PER_INTERVAL, suit.getMaxEnergy() - suit.getEnergy());
                        suit.setEnergy(suit.getEnergy() + toGive);
                        if (!player.isCreative()) entity.energyStorage.extractEnergy(toGive, false);
                        ModMessages.sendSyncPacket(player);
                    } else if (!suit.getActiveReactorType().equals("none")) {
                        // Physical reactor: energy is stored as SuitEnergy in chestplate NBT.
                        // BaseSuitItem reads that NBT every tick and overwrites suit.energy,
                        // so we must patch the item directly.
                        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
                        if (!chest.isEmpty() && chest.hasTag()) {
                            int current = chest.getOrCreateTag().getInt("SuitEnergy");
                            int max     = chest.getOrCreateTag().getInt("SuitMaxEnergy");
                            if (max > 0 && current < max) {
                                int toGive = Math.min(RECHARGE_PER_INTERVAL, max - current);
                                chest.getOrCreateTag().putInt("SuitEnergy", current + toGive);
                                if (!player.isCreative()) entity.energyStorage.extractEnergy(toGive, false);
                                ModMessages.sendSyncPacket(player);
                            }
                        }
                    }
                }
            });
        }

        List<Monster> hostiles = level.getEntitiesOfClass(Monster.class, zone,
                m -> m.distanceToSqr(pos.getCenter()) <= RANGE * RANGE);
        for (Monster monster : hostiles) {
            monster.addEffect(new MobEffectInstance(MobEffects.GLOWING, TICK_INTERVAL + 10, 0, false, false, false));
        }
    }
}
