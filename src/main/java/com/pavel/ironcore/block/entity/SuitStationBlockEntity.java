package com.pavel.ironcore.block.entity;

import com.pavel.ironcore.item.module.ModuleItem;
import com.pavel.ironcore.item.module.SuitModules;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Станция хранит только нагрудник. Слоты модулей в GUI — «окно» в NBT этого нагрудника
 * ({@link StationHandler}), поэтому модули уезжают вместе с костюмом, а при поломке станции
 * выпадает один нагрудник (без дублирования модулей).
 */
public class SuitStationBlockEntity extends AbstractMachineBlockEntity {
    public static final int CHEST_SLOT = 0;
    public static final int FIRST_MODULE_SLOT = 1;

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() instanceof ArmorItem &&
                   ((ArmorItem) stack.getItem()).getType() == ArmorItem.Type.CHESTPLATE;
        }
    };

    private final StationHandler stationHandler = new StationHandler();

    public SuitStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUIT_STATION_BE.get(), pos, state);
    }

    @Override
    protected ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    /** Нагрудник + слоты его модулей — для меню станции. */
    public IItemHandlerModifiable getStationHandler() {
        return stationHandler;
    }

    @Override
    protected void loadInventory(CompoundTag tag) {
        // Migration guard: old saves may have had 3 slots (reactor install era).
        ItemStackHandler tempHandler = new ItemStackHandler(3);
        tempHandler.deserializeNBT(tag.getCompound("inventory"));
        itemHandler.setSize(1);
        itemHandler.setStackInSlot(0, tempHandler.getStackInSlot(0));
    }

    private final class StationHandler implements IItemHandlerModifiable {
        private ItemStack chest() {
            return itemHandler.getStackInSlot(CHEST_SLOT);
        }

        private int moduleIndex(int slot) {
            return slot - FIRST_MODULE_SLOT;
        }

        @Override
        public int getSlots() {
            return FIRST_MODULE_SLOT + SuitModules.MAX_SLOTS;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            if (slot == CHEST_SLOT) return chest();
            return SuitModules.getModuleStack(chest(), moduleIndex(slot));
        }

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            if (slot == CHEST_SLOT) {
                itemHandler.setStackInSlot(CHEST_SLOT, stack);
                return;
            }
            ItemStack chest = chest();
            if (chest.isEmpty()) return;
            SuitModules.setModuleStack(chest, moduleIndex(slot), stack);
            setChanged();
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (slot == CHEST_SLOT) return itemHandler.insertItem(CHEST_SLOT, stack, simulate);
            if (stack.isEmpty() || !getStackInSlot(slot).isEmpty() || !isItemValid(slot, stack)) return stack;
            if (!simulate) setStackInSlot(slot, stack.copyWithCount(1));
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot == CHEST_SLOT) return itemHandler.extractItem(CHEST_SLOT, amount, simulate);
            ItemStack existing = getStackInSlot(slot);
            if (amount <= 0 || existing.isEmpty()) return ItemStack.EMPTY;
            if (!simulate) setStackInSlot(slot, ItemStack.EMPTY);
            return existing;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == CHEST_SLOT) return itemHandler.isItemValid(CHEST_SLOT, stack);
            if (!(stack.getItem() instanceof ModuleItem module)) return false;
            ItemStack chest = chest();
            if (moduleIndex(slot) >= SuitModules.slotsFor(chest)) return false;
            // Один модуль каждого типа на костюм
            for (int i = 0; i < SuitModules.MAX_SLOTS; i++) {
                if (i != moduleIndex(slot) && SuitModules.getModuleStack(chest, i).getItem() instanceof ModuleItem other
                        && other.getModule() == module.getModule()) {
                    return false;
                }
            }
            return true;
        }
    }
}
