package com.pavel.ironcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.inventory.ContainerData;

public class CoalGeneratorBlockEntity extends BlockEntity {
    public final ContainerData data;
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) { // 1 слот для топлива
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final CustomEnergyStorage energyStorage = new CustomEnergyStorage(100000, 1000);

    private class CustomEnergyStorage extends EnergyStorage {
        public CustomEnergyStorage(int capacity, int maxExtract) {
            super(capacity, 0, maxExtract);
        }
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            setChanged();
            return super.extractEnergy(maxExtract, simulate);
        }
        public void generateEnergy(int amount) {
            this.energy += amount;
            if(this.energy > this.capacity) this.energy = this.capacity;
            setChanged();
        }
    }

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    public int burnTime = 0;
    public int maxBurnTime = 0;

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_GENERATOR_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                switch (index) {
                    case 0: return CoalGeneratorBlockEntity.this.burnTime;
                    case 1: return CoalGeneratorBlockEntity.this.maxBurnTime;
                    case 2: return CoalGeneratorBlockEntity.this.energyStorage.getEnergyStored();
                    case 3: return CoalGeneratorBlockEntity.this.energyStorage.getMaxEnergyStored();
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0: CoalGeneratorBlockEntity.this.burnTime = value; break;
                    case 1: CoalGeneratorBlockEntity.this.maxBurnTime = value; break;
                }
            }

            @Override
            public int getCount() {
                return 4;
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
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putInt("energy", energyStorage.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        energyStorage.receiveEnergy(tag.getInt("energy"), false);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, CoalGeneratorBlockEntity entity) {
        if (level.isClientSide) return;

        boolean isBurning = entity.burnTime > 0;
        boolean changed = false;

        // Генерация энергии
        if (entity.burnTime > 0) {
            entity.burnTime--;
            // Генерируем 60 FE за тик
            if (entity.energyStorage.getEnergyStored() < entity.energyStorage.getMaxEnergyStored()) {
                entity.energyStorage.generateEnergy(60);
                changed = true;
            }
        } else {
            // Пытаемся зажечь новое топливо
            ItemStack fuelStack = entity.itemHandler.getStackInSlot(0);
            if (!fuelStack.isEmpty() && entity.energyStorage.getEnergyStored() < entity.energyStorage.getMaxEnergyStored()) {
                int burnDuration = ForgeHooks.getBurnTime(fuelStack, RecipeType.SMELTING);
                if (burnDuration > 0) {
                    entity.burnTime = burnDuration;
                    entity.maxBurnTime = burnDuration;
                    fuelStack.shrink(1);
                    changed = true;
                }
            }
        }

        // Передача энергии соседям (например, Alloy Smelter)
        if (entity.energyStorage.getEnergyStored() > 0) {
            for (Direction direction : Direction.values()) {
                BlockEntity neighbor = level.getBlockEntity(pos.relative(direction));
                if (neighbor != null) {
                    neighbor.getCapability(ForgeCapabilities.ENERGY, direction.getOpposite()).ifPresent(neighborEnergy -> {
                        if (neighborEnergy.canReceive()) {
                            int toSend = Math.min(entity.energyStorage.getEnergyStored(), 1000); // Max 1000 FE/t transfer
                            int received = neighborEnergy.receiveEnergy(toSend, false);
                            entity.energyStorage.extractEnergy(received, false);
                        }
                    });
                }
            }
        }

        if (changed) {
            setChanged(level, pos, state);
        }
    }
}
