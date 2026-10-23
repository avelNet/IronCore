package com.pavel.ironcore.screen;

import com.pavel.ironcore.block.ModBlocks;
import com.pavel.ironcore.block.entity.SuitStationBlockEntity;
import com.pavel.ironcore.item.module.SuitModules;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SuitStationMenu extends AbstractMachineMenu {
    public static final int CHEST_X = 26;
    public static final int MODULE_X = 80;
    public static final int SLOT_Y = 35;

    public final SuitStationBlockEntity blockEntity;
    private final Level level;

    public SuitStationMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public SuitStationMenu(int pContainerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.SUIT_STATION_MENU.get(), pContainerId);
        blockEntity = (SuitStationBlockEntity) entity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        IItemHandler handler = blockEntity.getStationHandler();
        this.addSlot(new SlotItemHandler(handler, SuitStationBlockEntity.CHEST_SLOT, CHEST_X, SLOT_Y));
        for (int i = 0; i < SuitModules.MAX_SLOTS; i++) {
            this.addSlot(new ModuleSlot(handler, SuitStationBlockEntity.FIRST_MODULE_SLOT + i, MODULE_X + i * 18, SLOT_Y, i));
        }
    }

    public ItemStack getChest() {
        return blockEntity.getStationHandler().getStackInSlot(SuitStationBlockEntity.CHEST_SLOT);
    }

    @Override
    protected int getMachineSlotCount() {
        return 1 + SuitModules.MAX_SLOTS;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.SUIT_STATION.get());
    }

    /** Слот модуля активен, только если у вставленного нагрудника столько слотов есть. */
    private final class ModuleSlot extends SlotItemHandler {
        private final int moduleIndex;

        ModuleSlot(IItemHandler handler, int index, int x, int y, int moduleIndex) {
            super(handler, index, x, y);
            this.moduleIndex = moduleIndex;
        }

        @Override
        public boolean isActive() {
            return moduleIndex < SuitModules.slotsFor(getChest());
        }
    }
}
