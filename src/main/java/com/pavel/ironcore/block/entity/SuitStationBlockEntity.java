package com.pavel.ironcore.block.entity;

import com.pavel.ironcore.item.ReactorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SuitStationBlockEntity extends BlockEntity {
    private final ItemStackHandler itemHandler = new ItemStackHandler(2) { // 0: Chestplate, 1: Reactor
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) return stack.getItem() instanceof ArmorItem && ((ArmorItem)stack.getItem()).getType() == ArmorItem.Type.CHESTPLATE;
            if (slot == 1) return stack.getItem() instanceof ReactorItem;
            return super.isItemValid(slot, stack);
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    public SuitStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUIT_STATION_BE.get(), pos, state);
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

    public static void tick(Level level, BlockPos pos, BlockState state, SuitStationBlockEntity entity) {
        if (level.isClientSide()) return;

        ItemStack chestplate = entity.itemHandler.getStackInSlot(0);
        ItemStack reactor = entity.itemHandler.getStackInSlot(1);

        // Логика установки реактора
        if (!chestplate.isEmpty() && !reactor.isEmpty()) {
            // Проверяем, что в нагруднике еще нет реактора (или мы его меняем)
            // В данной итерации просто "вплавляем" реактор при наличии обоих предметов
            
            if (reactor.getItem() instanceof ReactorItem reactorItem) {
                CompoundTag tag = chestplate.getOrCreateTag();
                
                // Переносим данные
                tag.putString("InstalledReactor", reactor.getItem().toString()); // Упрощенно для альфы
                
                // Получаем энергию из реактора через его капку
                reactor.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
                    tag.putInt("SuitEnergy", energy.getEnergyStored());
                    tag.putInt("SuitMaxEnergy", energy.getMaxEnergyStored());
                });

                // Удаляем предмет реактора
                entity.itemHandler.extractItem(1, 1, false);
                entity.setChanged();
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        tag.put("inventory", itemHandler.serializeNBT());
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        itemHandler.deserializeNBT(tag.getCompound("inventory"));
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
}
