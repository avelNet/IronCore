package com.pavel.ironcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class SuitStationBlockEntity extends AbstractMachineBlockEntity {
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

    public SuitStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SUIT_STATION_BE.get(), pos, state);
    }

    @Override
    protected ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    protected void loadInventory(CompoundTag tag) {
        // Migration guard: old saves may have had 3 slots (reactor install era).
        ItemStackHandler tempHandler = new ItemStackHandler(3);
        tempHandler.deserializeNBT(tag.getCompound("inventory"));
        itemHandler.setSize(1);
        itemHandler.setStackInSlot(0, tempHandler.getStackInSlot(0));
    }
}
