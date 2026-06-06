package com.pavel.ironcore.item;

import com.pavel.ironcore.IronCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import com.pavel.ironcore.block.ModBlocks;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IronCore.MODID);

    public static final RegistryObject<CreativeModeTab> IRONCORE_TAB = CREATIVE_MODE_TABS.register("ironcore_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.MK2_HELMET.get()))
                    .title(Component.translatable("creativetab.ironcore_tab"))
                    .displayItems((parameters, output) -> {
                        // Оружие и броня
                        output.accept(ModItems.MK1_HELMET.get());
                        output.accept(ModItems.MK1_CHESTPLATE.get());
                        output.accept(ModItems.MK1_LEGGINGS.get());
                        output.accept(ModItems.MK1_BOOTS.get());
                        
                        output.accept(ModItems.MK2_HELMET.get());
                        output.accept(ModItems.MK2_CHESTPLATE.get());
                        output.accept(ModItems.MK2_LEGGINGS.get());
                        output.accept(ModItems.MK2_BOOTS.get());
                        
                        // Энергетика
                        output.accept(ModItems.COAL_REACTOR.get());

                        // Индустрия
                        output.accept(ModItems.RAW_TITANIUM.get());
                        output.accept(ModItems.TITANIUM_INGOT.get());
                        output.accept(ModBlocks.TITANIUM_ORE.get());
                        output.accept(ModBlocks.DEEPSLATE_TITANIUM_ORE.get());
                        
                        output.accept(ModItems.RAW_PALLADIUM.get());
                        output.accept(ModItems.PALLADIUM_INGOT.get());
                        output.accept(ModBlocks.PALLADIUM_ORE.get());
                    }).build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
