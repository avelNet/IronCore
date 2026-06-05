package com.pavel.ironcore.item;

import com.pavel.ironcore.IronCore;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, IronCore.MODID);

    public static final RegistryObject<Item> MK1_FRAME = ITEMS.register("mk1_frame",
            () -> new Mk1FrameItem(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, new Item.Properties()));

    public static final RegistryObject<Item> COAL_REACTOR = ITEMS.register("coal_reactor",
            () -> new ReactorItem(new Item.Properties().stacksTo(1), 5000, "coal"));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
