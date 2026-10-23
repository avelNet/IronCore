package com.pavel.ironcore.item.module;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ModuleItem extends Item {
    private final SuitModule module;

    public ModuleItem(SuitModule module, Properties properties) {
        super(properties.stacksTo(1));
        this.module = module;
    }

    public SuitModule getModule() {
        return module;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ironcore.module_category." + module.category().name().toLowerCase())
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable(module.descriptionKey()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ironcore.module_install").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
