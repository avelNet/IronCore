package com.pavel.ironcore.screen;

import com.pavel.ironcore.IronCore;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, IronCore.MODID);

    public static final RegistryObject<MenuType<AlloySmelterMenu>> ALLOY_SMELTER_MENU =
            registerMenuType("alloy_smelter_menu", AlloySmelterMenu::new);

    public static final RegistryObject<MenuType<CoalGeneratorMenu>> COAL_GENERATOR_MENU =
            registerMenuType("coal_generator_menu", CoalGeneratorMenu::new);

    private static <T extends AbstractContainerMenu> RegistryObject<MenuType<T>> registerMenuType(String name, IContainerFactory<T> factory) {
        return MENUS.register(name, () -> IForgeMenuType.create(factory));
    }

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
