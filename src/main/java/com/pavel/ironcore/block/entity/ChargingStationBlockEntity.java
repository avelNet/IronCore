package com.pavel.ironcore.block.entity;

import com.pavel.ironcore.item.ReactorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ChargingStationBlockEntity extends BlockEntity {
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

    private final CustomEnergyStorage energyStorage = new CustomEnergyStorage(1000000, 10000);

    private class CustomEnergyStorage extends EnergyStorage {
        public CustomEnergyStorage(int capacity, int maxReceive) {
            super(capacity, maxReceive, 0);
        }
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            setChanged();
            return super.receiveEnergy(maxReceive, simulate);
        }
        public int extractInternal(int maxExtract, boolean simulate) {
            int energyExtracted = Math.min(energy, maxExtract);
            if (!simulate) {
                energy -= energyExtracted;
                setChanged();
            }
            return energyExtracted;
        }
        public void setEnergyDirect(int amount) {
            this.energy = Math.max(0, Math.min(amount, capacity));
        }
    }

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

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
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) return lazyItemHandler.cast();
        if(cap == ForgeCapabilities.ENERGY) return lazyEnergyHandler.cast();
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyEnergyHandler = LazyOptional.of(() -> energyStorage);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyEnergyHandler.invalidate();
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        tag.putInt("energy", energyStorage.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
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
