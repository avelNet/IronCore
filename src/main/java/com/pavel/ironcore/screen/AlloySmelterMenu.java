package com.pavel.ironcore.screen;

import net.minecraft.world.level.Level;
import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.block.ModBlocks;
import com.pavel.ironcore.block.entity.AlloySmelterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class AlloySmelterMenu extends AbstractContainerMenu {
    public final AlloySmelterBlockEntity blockEntity;
    private final Level level;

    public AlloySmelterMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AlloySmelterMenu(int pContainerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.ALLOY_SMELTER_MENU.get(), pContainerId);
        checkContainerSize(inv, 3);
        blockEntity = ((AlloySmelterBlockEntity) entity);
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new SlotItemHandler(iItemHandler, 0, 56, 17)); // Input 1
            this.addSlot(new SlotItemHandler(iItemHandler, 1, 56, 53)); // Input 2
            this.addSlot(new SlotItemHandler(iItemHandler, 2, 116, 35)); // Output
        });
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < 36) { // Инвентарь игрока -> Печка
            if (!moveItemStackTo(sourceStack, 36, 38, false)) { // Пытаемся положить в инпуты
                return ItemStack.EMPTY;
            }
        } else if (index < 39) { // Печка -> Инвентарь игрока
            if (!moveItemStackTo(sourceStack, 0, 36, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(playerIn, sourceStack);
        return copyOfSourceStack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.ALLOY_SMELTER.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }
}
