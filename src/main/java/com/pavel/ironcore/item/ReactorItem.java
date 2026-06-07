package com.pavel.ironcore.item;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReactorItem extends Item {
    private final int capacity;
    private final String reactorType;

    public ReactorItem(Properties properties, int capacity, String reactorType) {
        super(properties);
        this.capacity = capacity;
        this.reactorType = reactorType;
    }

    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable net.minecraft.nbt.CompoundTag nbt) {
        return new ICapabilityProvider() {
            private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> new EnergyStorage(capacity) {
                @Override
                public int receiveEnergy(int maxReceive, boolean simulate) {
                    if (!canReceive()) return 0;
                    int energyStored = getEnergyStored();
                    int energyReceived = Math.min(capacity - energyStored, Math.min(this.maxReceive, maxReceive));
                    if (!simulate) {
                        stack.getOrCreateTag().putInt("Energy", energyStored + energyReceived);
                    }
                    return energyReceived;
                }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    if (!canExtract()) return 0;
                    int energyStored = getEnergyStored();
                    int energyExtracted = Math.min(energyStored, Math.min(this.maxExtract, maxExtract));
                    if (!simulate) {
                        stack.getOrCreateTag().putInt("Energy", energyStored - energyExtracted);
                    }
                    return energyExtracted;
                }

                @Override
                public int getEnergyStored() {
                    return stack.getOrCreateTag().getInt("Energy");
                }

                @Override
                public int getMaxEnergyStored() {
                    return capacity;
                }

                @Override
                public boolean canExtract() {
                    return true;
                }

                @Override
                public boolean canReceive() {
                    return true;
                }
            });

            @Override
            public @NotNull <T> LazyOptional<T> getCapability(@NotNull net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
                if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY) {
                    return energy.cast();
                }
                return LazyOptional.empty();
            }
        };
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        // Правый клик больше не устанавливает реактор. Используйте Suit Station.
        if (!level.isClientSide) {
             player.displayClientMessage(Component.literal("§cUse a Suit Station to install this reactor."), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ironcore.reactor_type", reactorType));
        int energy = stack.getOrCreateTag().getInt("Energy");
        tooltip.add(Component.literal("§bEnergy: " + energy + " / " + capacity + " FE"));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
