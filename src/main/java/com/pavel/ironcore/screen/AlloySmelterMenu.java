package com.pavel.ironcore.screen;

import net.minecraft.world.level.Level;
import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.block.ModBlocks;
import com.pavel.ironcore.block.entity.AlloySmelterBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.SlotItemHandler;

import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;

public class AlloySmelterMenu extends AbstractMachineMenu {
    public final AlloySmelterBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public AlloySmelterMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    public AlloySmelterMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.ALLOY_SMELTER_MENU.get(), pContainerId);
        checkContainerSize(inv, 3);
        blockEntity = ((AlloySmelterBlockEntity) entity);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(iItemHandler -> {
            this.addSlot(new SlotItemHandler(iItemHandler, 0, 56, 17)); // Input 1
            this.addSlot(new SlotItemHandler(iItemHandler, 1, 56, 53)); // Input 2
            this.addSlot(new SlotItemHandler(iItemHandler, 2, 116, 35)); // Output
        });

        addDataSlots(data);
    }

    public boolean isCrafting() {
        return data.get(0) > 0;
    }

    public int getScaledProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);  // Max Progress
        int progressArrowSize = 24; // Width of the arrow

        return maxProgress != 0 && progress != 0 ? progress * progressArrowSize / maxProgress : 0;
    }

    public int getScaledEnergy() {
        int energy = this.data.get(2);
        int maxEnergy = this.data.get(3);
        int energyBarSize = 50; // Height of the energy bar

        return maxEnergy != 0 && energy != 0 ? energy * energyBarSize / maxEnergy : 0;
    }

    public int getEnergy() {
        return this.data.get(2);
    }

    public int getMaxEnergy() {
        return this.data.get(3);
    }

    @Override
    protected int getMachineSlotCount() {
        return 3;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.ALLOY_SMELTER.get());
    }
}
