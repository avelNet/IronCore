package com.pavel.ironcore.block.entity;

import com.pavel.ironcore.item.ReactorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class ChargingStationBlockEntity extends AbstractEnergyMachineBlockEntity {
    public final ContainerData data;
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof ReactorItem;
        }
    };

    private final ChargingEnergyStorage energyStorage = new ChargingEnergyStorage(1000000, 10000);

    private class ChargingEnergyStorage extends NotifyingEnergyStorage {
        public ChargingEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0, ChargingStationBlockEntity.this::setChanged);
        }
        public int extractInternal(int maxExtract, boolean simulate) {
            int energyExtracted = Math.min(energy, maxExtract);
            if (!simulate) {
                energy -= energyExtracted;
                setChanged();
            }
            return energyExtracted;
        }
    }

    public ChargingStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHARGING_STATION_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                switch (index) {
                    case 0: return ChargingStationBlockEntity.this.energyStorage.getEnergyStored();
                    case 1: return ChargingStationBlockEntity.this.energyStorage.getMaxEnergyStored();
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                // Energy is mostly handled by external sources
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
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

    public static void tick(Level level, BlockPos pos, BlockState state, ChargingStationBlockEntity entity) {
        if (level.isClientSide()) return;

        ItemStack stack = entity.itemHandler.getStackInSlot(0);
        if (!stack.isEmpty() && entity.energyStorage.getEnergyStored() > 0) {
            stack.getCapability(ForgeCapabilities.ENERGY).ifPresent(itemEnergy -> {
                if (itemEnergy.canReceive()) {
                    int toTransfer = Math.min(entity.energyStorage.getEnergyStored(), 5000); // 5000 FE/t charge rate
                    int received = itemEnergy.receiveEnergy(toTransfer, false);
                    entity.energyStorage.extractInternal(received, false);
                }
            });
        }
    }
}
