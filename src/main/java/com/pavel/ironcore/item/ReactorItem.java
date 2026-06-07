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
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                // В альфе просто пополняем энергию при клике
                suit.setEnergy(suit.getEnergy() + capacity / 10); // Пополняем по 10% за клик
                suit.setActiveReactorType(reactorType);
                player.displayClientMessage(Component.literal("Реактор установлен: " + reactorType.toUpperCase()), true);
            });
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ironcore.reactor_type", reactorType));
        tooltip.add(Component.translatable("tooltip.ironcore.capacity", capacity));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
