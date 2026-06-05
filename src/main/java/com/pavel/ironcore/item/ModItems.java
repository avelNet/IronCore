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

    // Полный сет Mk1
    public static final RegistryObject<Item> MK1_HELMET = ITEMS.register("mk1_helmet",
            () -> new Mk1FrameItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> MK1_CHESTPLATE = ITEMS.register("mk1_chestplate",
            () -> new Mk1FrameItem(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> MK1_LEGGINGS = ITEMS.register("mk1_leggings",
            () -> new Mk1FrameItem(ArmorMaterials.IRON, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> MK1_BOOTS = ITEMS.register("mk1_boots",
            () -> new Mk1FrameItem(ArmorMaterials.IRON, ArmorItem.Type.BOOTS, new Item.Properties()));

    // Полный сет Mk2
    public static final RegistryObject<Item> MK2_HELMET = ITEMS.register("mk2_helmet",
            () -> new Mk2FrameItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> MK2_CHESTPLATE = ITEMS.register("mk2_chestplate",
            () -> new Mk2FrameItem(ArmorMaterials.IRON, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> MK2_LEGGINGS = ITEMS.register("mk2_leggings",
            () -> new Mk2FrameItem(ArmorMaterials.IRON, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> MK2_BOOTS = ITEMS.register("mk2_boots",
            () -> new Mk2FrameItem(ArmorMaterials.IRON, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> COAL_REACTOR = ITEMS.register("coal_reactor",
            () -> new ReactorItem(new Item.Properties().stacksTo(1), 5000, "coal"));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
