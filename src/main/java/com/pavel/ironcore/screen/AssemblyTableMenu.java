package com.pavel.ironcore.screen;

import com.pavel.ironcore.block.ModBlocks;
import com.pavel.ironcore.block.entity.AssemblyTableBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

public class AssemblyTableMenu extends AbstractMachineMenu {
    public final AssemblyTableBlockEntity blockEntity;
    private final Level level;

    public AssemblyTableMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public AssemblyTableMenu(int pContainerId, Inventory inv, BlockEntity entity) {
        super(ModMenuTypes.ASSEMBLY_TABLE_MENU.get(), pContainerId);
        checkContainerSize(inv, 10);
        blockEntity = ((AssemblyTableBlockEntity) entity);
        this.level = inv.player.level();

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            // Сетка 3х3 (Слоты 0-8)
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    this.addSlot(new SlotItemHandler(iItemHandler, j + i * 3, 30 + j * 18, 17 + i * 18));
                }
            }
            // Результат (Слот 9)
            this.addSlot(new SlotItemHandler(iItemHandler, 9, 124, 35));
        });
    }

    @Override
    protected int getMachineSlotCount() {
        return 10;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.ASSEMBLY_TABLE.get());
    }
}
