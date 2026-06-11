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
            () -> new Mk1FrameItem(ModArmorMaterials.MK1, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> MK1_CHESTPLATE = ITEMS.register("mk1_chestplate",
            () -> new Mk1FrameItem(ModArmorMaterials.MK1, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> MK1_LEGGINGS = ITEMS.register("mk1_leggings",
            () -> new Mk1FrameItem(ModArmorMaterials.MK1, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> MK1_BOOTS = ITEMS.register("mk1_boots",
            () -> new Mk1FrameItem(ModArmorMaterials.MK1, ArmorItem.Type.BOOTS, new Item.Properties()));

    // Полный сет Mk2
    public static final RegistryObject<Item> MK2_HELMET = ITEMS.register("mk2_helmet",
            () -> new Mk2FrameItem(ModArmorMaterials.MK2, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> MK2_CHESTPLATE = ITEMS.register("mk2_chestplate",
            () -> new Mk2FrameItem(ModArmorMaterials.MK2, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> MK2_LEGGINGS = ITEMS.register("mk2_leggings",
            () -> new Mk2FrameItem(ModArmorMaterials.MK2, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> MK2_BOOTS = ITEMS.register("mk2_boots",
            () -> new Mk2FrameItem(ModArmorMaterials.MK2, ArmorItem.Type.BOOTS, new Item.Properties()));

    // Полный сет Mk3 (Золото-титановый сплав)
    public static final RegistryObject<Item> MK3_HELMET = ITEMS.register("mk3_helmet",
            () -> new Mk3FrameItem(ModArmorMaterials.MK3, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> MK3_CHESTPLATE = ITEMS.register("mk3_chestplate",
            () -> new Mk3FrameItem(ModArmorMaterials.MK3, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> MK3_LEGGINGS = ITEMS.register("mk3_leggings",
            () -> new Mk3FrameItem(ModArmorMaterials.MK3, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> MK3_BOOTS = ITEMS.register("mk3_boots",
            () -> new Mk3FrameItem(ModArmorMaterials.MK3, ArmorItem.Type.BOOTS, new Item.Properties()));

    public static final RegistryObject<Item> COAL_REACTOR = ITEMS.register("coal_reactor",
            () -> new ReactorItem(new Item.Properties().stacksTo(1), 5000, "coal"));

    public static final RegistryObject<Item> PALLADIUM_REACTOR = ITEMS.register("palladium_reactor",
            () -> new ReactorItem(new Item.Properties().stacksTo(1), 50000, "palladium"));

    public static final RegistryObject<Item> CHLOROPHYLL_JUICE = ITEMS.register("chlorophyll_juice",
            () -> new Item(new Item.Properties().food(new net.minecraft.world.food.FoodProperties.Builder().alwaysEat().nutrition(1).saturationMod(0.1f).build())) {
                @Override
                public net.minecraft.world.item.UseAnim getUseAnimation(net.minecraft.world.item.ItemStack stack) {
                    return net.minecraft.world.item.UseAnim.DRINK;
                }

                @Override
                public net.minecraft.sounds.SoundEvent getDrinkingSound() {
                    return net.minecraft.sounds.SoundEvents.GENERIC_DRINK;
                }

                @Override
                public net.minecraft.sounds.SoundEvent getEatingSound() {
                    return net.minecraft.sounds.SoundEvents.GENERIC_DRINK;
                }

                @Override
                public net.minecraft.world.item.ItemStack finishUsingItem(net.minecraft.world.item.ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity) {
                    if (!level.isClientSide && entity instanceof net.minecraft.world.entity.player.Player player) {
                        player.getCapability(com.pavel.ironcore.capability.SuitCapabilityProvider.SUIT_CAPABILITY).ifPresent(suit -> {
                            suit.setPalladiumPoisoning(suit.getPalladiumPoisoning() - 25.0f); // Снижаем на 25%
                            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§aToxicity reduced!"), true);
                        });
                        
                        if (!player.getAbilities().instabuild) {
                            if (stack.getCount() <= 1) {
                                return new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
                            }
                            player.getInventory().add(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE));
                        }
                    }
                    return super.finishUsingItem(stack, level, entity);
                }
            });

    // Индустриальные ресурсы
    public static final RegistryObject<Item> RAW_TITANIUM = ITEMS.register("raw_titanium",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TITANIUM_INGOT = ITEMS.register("titanium_ingot",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> RAW_PALLADIUM = ITEMS.register("raw_palladium",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PALLADIUM_INGOT = ITEMS.register("palladium_ingot",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> GOLD_TITANIUM_ALLOY = ITEMS.register("gold_titanium_alloy",
            () -> new Item(new Item.Properties()));
            
    public static final RegistryObject<Item> PALLADIUM_CORE = ITEMS.register("palladium_core",
            () -> new PalladiumCoreItem(new Item.Properties().stacksTo(16)));
            
    public static final RegistryObject<Item> DEPLETED_PALLADIUM_CORE = ITEMS.register("depleted_palladium_core",
            () -> new Item(new Item.Properties().stacksTo(16)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
