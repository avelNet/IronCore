package com.pavel.ironcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;

public class CoalGeneratorBlockEntity extends AbstractEnergyMachineBlockEntity {
    public final ContainerData data;
    private final ItemStackHandler itemHandler = new ItemStackHandler(1) { // 1 слот для топлива
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final CoalEnergyStorage energyStorage = new CoalEnergyStorage(100000, 1000);

    private class CoalEnergyStorage extends NotifyingEnergyStorage {
        public CoalEnergyStorage(int capacity, int maxExtract) {
            super(capacity, 0, maxExtract, CoalGeneratorBlockEntity.this::setChanged);
        }
        public void generateEnergy(int amount) {
            this.energy += amount;
            if (this.energy > this.capacity) this.energy = this.capacity;
            setChanged();
        }
    }

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
        tag.putInt("burnTime", burnTime);
        tag.putInt("maxBurnTime", maxBurnTime);
        tag.putInt("energy", energyStorage.getEnergyStored());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        burnTime = tag.getInt("burnTime");
        maxBurnTime = tag.getInt("maxBurnTime");
        energyStorage.setEnergyDirect(tag.getInt("energy"));
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
