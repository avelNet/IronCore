package com.pavel.ironcore.screen;

import com.pavel.ironcore.block.ModBlocks;
import com.pavel.ironcore.block.entity.SuitStationBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class SuitStationMenu extends AbstractContainerMenu {
    public final SuitStationBlockEntity blockEntity;
    private final Level level;

    public SuitStationMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public SuitStationMenu(int pContainerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.SUIT_STATION_MENU.get(), pContainerId);
        checkContainerSize(inv, 3);
        blockEntity = ((SuitStationBlockEntity) entity);
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            // Слот для Нагрудника (Слот 0) - Центр
            this.addSlot(new SlotItemHandler(iItemHandler, 0, 56, 35));
            // Слот для ВХОДЯЩЕГО Реактора (Слот 1) - Справа сверху
            this.addSlot(new SlotItemHandler(iItemHandler, 1, 116, 17));
            // Слот для ИЗВЛЕЧЕННОГО Реактора (Слот 2) - Справа снизу
            this.addSlot(new SlotItemHandler(iItemHandler, 2, 116, 53) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false; // Запрещаем класть руками в слот выхода
                }
            });
        });
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        Slot sourceSlot = slots.get(index);
        if (sourceSlot == null || !sourceSlot.hasItem()) return ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack copyOfSourceStack = sourceStack.copy();

        if (index < 36) { // Инвентарь -> Стенд
            if (!moveItemStackTo(sourceStack, 36, 38, false)) { // Пытаемся положить в слоты 0 или 1
                return ItemStack.EMPTY;
            }
        } else if (index < 39) { // Стенд -> Инвентарь
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
                player, ModBlocks.SUIT_STATION.get());
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
