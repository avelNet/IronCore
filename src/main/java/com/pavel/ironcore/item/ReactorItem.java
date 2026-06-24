package com.pavel.ironcore.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Палладиевый реактор. В имплант-модели это разовый триггер вживления:
 * правый клик обрабатывается в {@link com.pavel.ironcore.event.StoryEvents}
 * (запуск хирургической процедуры), после чего предмет расходуется и реактор
 * становится встроенным имплантом. Поэтому собственной логики у предмета нет.
 */
public class ReactorItem extends Item {
    private final String reactorType;

    public ReactorItem(Properties properties, String reactorType) {
        super(properties);
        this.reactorType = reactorType;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.ironcore.reactor_type", reactorType));
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
