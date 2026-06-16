package com.pavel.ironcore.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;

public class AssemblyTableBlockEntity extends AbstractMachineBlockEntity {
    private final ItemStackHandler itemHandler = new ItemStackHandler(10) { // 9 слотов сетки + 1 результат
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLY_TABLE_BE.get(), pos, state);
    }

    @Override
    protected ItemStackHandler getItemHandler() {
        return itemHandler;
    }
}
