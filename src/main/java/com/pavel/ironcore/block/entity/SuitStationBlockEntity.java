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
    private final ItemStackHandler itemHandler = new ItemStackHandler(3) { // 0: Chestplate, 1: Reactor IN, 2: Reactor OUT
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == 0) return stack.getItem() instanceof ArmorItem && ((ArmorItem)stack.getItem()).getType() == ArmorItem.Type.CHESTPLATE;
            if (slot == 1) return stack.getItem() instanceof ReactorItem;
            if (slot == 2) return stack.getItem() instanceof ReactorItem; // Разрешаем программно класть реакторы
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
        ItemStack reactorIn = entity.itemHandler.getStackInSlot(1);
        ItemStack reactorOut = entity.itemHandler.getStackInSlot(2);

        if (!chestplate.isEmpty() && chestplate.getItem() instanceof ArmorItem) {
            CompoundTag chestTag = chestplate.getOrCreateTag();
            String installedType = chestTag.getString("InstalledReactor");
            int storedEnergy = chestTag.getInt("SuitEnergy");

            // ИЗВЛЕЧЕНИЕ: Если в броне есть реактор, а слот ВЫХОДА (2) пуст
            if (!installedType.isEmpty() && !installedType.equals("none") && reactorOut.isEmpty()) {
                net.minecraft.world.item.Item reactorItemToSpawn = null;
                if (installedType.contains("palladium")) {
                    reactorItemToSpawn = com.pavel.ironcore.item.ModItems.PALLADIUM_REACTOR.get();
                } else if (installedType.contains("coal")) {
                    reactorItemToSpawn = com.pavel.ironcore.item.ModItems.COAL_REACTOR.get();
                }

                if (reactorItemToSpawn != null) {
                    ItemStack extractedReactor = new ItemStack(reactorItemToSpawn);
                    extractedReactor.getOrCreateTag().putInt("Energy", storedEnergy);
                    
                    // Кладем старый реактор в слот ВЫХОДА (2) напрямую, обходя insertItem
                    entity.itemHandler.setStackInSlot(2, extractedReactor);
                    
                    // Очищаем данные в нагруднике
                    chestTag.putString("InstalledReactor", "none");
                    chestTag.putInt("SuitEnergy", 0);
                    chestTag.putInt("SuitMaxEnergy", 0);
                    entity.setChanged();
                }
            } 
            // УСТАНОВКА: Если броня пустая, а в слоте ВХОДА (1) есть реактор
            else if ((installedType.isEmpty() || installedType.equals("none")) && !reactorIn.isEmpty() && reactorIn.getItem() instanceof ReactorItem) {
                chestTag.putString("InstalledReactor", reactorIn.getItem().toString()); 
                
                reactorIn.getCapability(ForgeCapabilities.ENERGY).ifPresent(energy -> {
                    chestTag.putInt("SuitEnergy", energy.getEnergyStored());
                    chestTag.putInt("SuitMaxEnergy", energy.getMaxEnergyStored());
                });

                // Удаляем предмет реактора из слота ВХОДА (1)
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
        
        // ЗАЩИТА ОТ ВЫЛЕТОВ (Обратная совместимость)
        // Если блок был сохранен в мире, когда у него было только 2 слота,
        // NBT уменьшит размер инвентаря до 2. Принудительно возвращаем 3.
        if (itemHandler.getSlots() < 3) {
            itemHandler.setSize(3);
        }
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }
}
