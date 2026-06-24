package com.pavel.ironcore.screen;

import com.pavel.ironcore.block.ModBlocks;
import com.pavel.ironcore.block.entity.SuitStationBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
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
        checkContainerSize(inv, 1);
        blockEntity = (SuitStationBlockEntity) entity;
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new SlotItemHandler(iItemHandler, 0, 80, 35));
        });
    }

    @Override
    protected int getMachineSlotCount() {
        return 1;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.SUIT_STATION.get());
    }
}
