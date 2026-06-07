package com.pavel.ironcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.world.inventory.ContainerData;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

public class AlloySmelterBlockEntity extends BlockEntity {
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) { // 0: Слот 1, 1: Слот 2, 2: Результат
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final EnergyStorage energyStorage = new EnergyStorage(50000, 1000, 0) {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            setChanged();
            return super.receiveEnergy(maxReceive, simulate);
        }
        
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            setChanged();
            return super.extractEnergy(maxExtract, simulate);
        }
    };
    
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> lazyEnergyHandler = LazyOptional.empty();

    public final ContainerData data;
    private int progress = 0;
    private int maxProgress = 100; // 5 секунд (100 тиков) на одну операцию
    
    private static final int ENERGY_REQ = 50; // 50 FE за тик (Всего 5000 FE за операцию)

    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_SMELTER_BE.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                switch (index) {
                    case 0: return AlloySmelterBlockEntity.this.progress;
                    case 1: return AlloySmelterBlockEntity.this.maxProgress;
                    case 2: return AlloySmelterBlockEntity.this.energyStorage.getEnergyStored();
                    case 3: return AlloySmelterBlockEntity.this.energyStorage.getMaxEnergyStored();
                    default: return 0;
                }
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0: AlloySmelterBlockEntity.this.progress = value; break;
                    case 1: AlloySmelterBlockEntity.this.maxProgress = value; break;
                    // Клиент не должен изменять энергию сервера напрямую
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        if(cap == ForgeCapabilities.ENERGY) {
            return lazyEnergyHandler.cast();
        }
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
        tag.putInt("alloy_smelter.progress", progress);
        tag.putInt("alloy_smelter.energy", energyStorage.getEnergyStored());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("alloy_smelter.progress");
        energyStorage.receiveEnergy(tag.getInt("alloy_smelter.energy"), false);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AlloySmelterBlockEntity pEntity) {
        if (level.isClientSide()) return;

        if (hasRecipe(pEntity) && pEntity.energyStorage.getEnergyStored() >= ENERGY_REQ) {
            pEntity.energyStorage.extractEnergy(ENERGY_REQ, false);
            pEntity.progress++;
            setChanged(level, pos, state);
            if (pEntity.progress >= pEntity.maxProgress) {
                craftItem(pEntity);
            }
        } else {
            pEntity.resetProgress();
            setChanged(level, pos, state);
        }
    }

    private void resetProgress() {
        this.progress = 0;
    }

    private static boolean hasRecipe(AlloySmelterBlockEntity entity) {
        Level level = entity.level;
        if (level == null) return false;

        // Slot 0: Золото (слиток или сырое), Slot 1: Титан (слиток или сырой)
        // Для альфы сделаем простую проверку: Слиток золота + Слиток титана -> Палладий (пока нет предмета Сплава, будем делать Палладий для тестов)
        // ВАЖНО: В будущем мы добавим отдельный предмет "Gold-Titanium Alloy"
        
        Item slot0 = entity.itemHandler.getStackInSlot(0).getItem();
        Item slot1 = entity.itemHandler.getStackInSlot(1).getItem();
        Item gold = net.minecraft.world.item.Items.GOLD_INGOT;
        Item titanium = com.pavel.ironcore.item.ModItems.TITANIUM_INGOT.get();

        boolean hasInput = (slot0 == gold && slot1 == titanium) || (slot0 == titanium && slot1 == gold);

        if (hasInput) {
            ItemStack resultSlot = entity.itemHandler.getStackInSlot(2);
            // Проверяем, что слот выхода пуст или там уже лежит нужный предмет и есть место
            return resultSlot.isEmpty() || 
                   (resultSlot.getItem() == com.pavel.ironcore.item.ModItems.PALLADIUM_INGOT.get() && resultSlot.getCount() < resultSlot.getMaxStackSize());
        }
        return false;
    }

    private static void craftItem(AlloySmelterBlockEntity entity) {
        entity.itemHandler.extractItem(0, 1, false);
        entity.itemHandler.extractItem(1, 1, false);

        ItemStack resultStack = new ItemStack(com.pavel.ironcore.item.ModItems.PALLADIUM_INGOT.get(), 1); // Делаем Палладий для теста
        
        if (entity.itemHandler.getStackInSlot(2).isEmpty()) {
            entity.itemHandler.setStackInSlot(2, resultStack);
        } else {
            entity.itemHandler.getStackInSlot(2).grow(resultStack.getCount());
        }
        
        entity.resetProgress();
    }
}
