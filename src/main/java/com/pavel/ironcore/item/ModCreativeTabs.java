package com.pavel.ironcore.item;

import com.pavel.ironcore.IronCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IronCore.MODID);

    public static final RegistryObject<CreativeModeTab> IRONCORE_TAB = CREATIVE_MODE_TABS.register("ironcore_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MK2_HELMET.get()))
                    .title(Component.translatable("creativetab.ironcore_tab"))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.MK1_HELMET.get());
                        output.accept(ModItems.MK1_CHESTPLATE.get());
                        output.accept(ModItems.MK1_LEGGINGS.get());
                        output.accept(ModItems.MK1_BOOTS.get());
                        
                        output.accept(ModItems.MK2_HELMET.get());
                        output.accept(ModItems.MK2_CHESTPLATE.get());
                        output.accept(ModItems.MK2_LEGGINGS.get());
                        output.accept(ModItems.MK2_BOOTS.get());
                        
                        output.accept(ModItems.COAL_REACTOR.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
