package com.pavel.ironcore.item;

import com.pavel.ironcore.capability.SuitCapabilityProvider;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

public class PalladiumCoreItem extends Item {
    public PalladiumCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        
        if (!level.isClientSide()) {
            player.getCapability(SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                if (suit.hasEmbeddedReactor()) {
                    int chargeAmount = 50000; 
                    suit.setMaxEnergy(chargeAmount);
                    suit.setEnergy(chargeAmount);
                    
                    // Записываем энергию в тег нагрудника, чтобы BaseSuitItem не перезатёр
                    net.minecraft.world.item.ItemStack chestplate = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
                    if (!chestplate.isEmpty()) {
                        chestplate.getOrCreateTag().putInt("SuitEnergy", chargeAmount);
                        chestplate.getOrCreateTag().putInt("SuitMaxEnergy", chargeAmount);
                    }
                    
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                        ItemStack depleted = new ItemStack(ModItems.DEPLETED_PALLADIUM_CORE.get());
                        if (!player.getInventory().add(depleted)) {
                            player.drop(depleted, false);
                        }
                    }
                    
                    level.playSound(null, player.blockPosition(), SoundEvents.IRON_GOLEM_REPAIR, SoundSource.PLAYERS, 1.0f, 1.0f);
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§b[СИСТЕМА]: Заменен топливный стержень."), true);
                } else {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cНет порта для установки стержня!"), true);
                }
            });
        }
        
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }
}
