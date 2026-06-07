package com.pavel.ironcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AlloySmelterBlockEntity extends BlockEntity {
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) { // 0: Слот 1, 1: Слот 2, 2: Результат
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private int progress = 0;
    private int maxProgress = 100; // 5 секунд (100 тиков) на одну операцию
    
    // В будущем тут будет расход энергии
    private static final int ENERGY_REQ = 500;

    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_SMELTER_BE.get(), pos, state);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
        if(cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
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
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
        progress = tag.getInt("alloy_smelter.progress");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AlloySmelterBlockEntity pEntity) {
        if (level.isClientSide()) return;

        if (hasRecipe(pEntity)) {
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
        
        boolean hasInput1 = entity.itemHandler.getStackInSlot(0).getItem() == net.minecraft.world.item.Items.GOLD_INGOT;
        boolean hasInput2 = entity.itemHandler.getStackInSlot(1).getItem() == com.pavel.ironcore.item.ModItems.TITANIUM_INGOT.get();

        if (hasInput1 && hasInput2) {
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
