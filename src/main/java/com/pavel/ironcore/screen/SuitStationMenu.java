package com.pavel.ironcore.screen;

import com.pavel.ironcore.block.ModBlocks;
import com.pavel.ironcore.block.entity.SuitStationBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class SuitStationMenu extends AbstractMachineMenu {
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
    protected int getMachineSlotCount() {
        return 3;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.SUIT_STATION.get());
    }
}
