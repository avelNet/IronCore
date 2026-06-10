package com.pavel.ironcore.item;

import com.pavel.ironcore.IronCore;
import net.minecraft.Util;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.EnumMap;
import java.util.function.Supplier;

public enum ModArmorMaterials implements ArmorMaterial {
    MK1("mk1", 35, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266652_) -> {
        p_266652_.put(ArmorItem.Type.BOOTS, 2);
        p_266652_.put(ArmorItem.Type.LEGGINGS, 5);
        p_266652_.put(ArmorItem.Type.CHESTPLATE, 6);
        p_266652_.put(ArmorItem.Type.HELMET, 2);
    }), 12, SoundEvents.ARMOR_EQUIP_IRON, 2.0F, 0.0F, () -> Ingredient.of(ModItems.TITANIUM_INGOT.get())),
    
    MK2("mk2", 60, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266652_) -> {
        p_266652_.put(ArmorItem.Type.BOOTS, 3);
        p_266652_.put(ArmorItem.Type.LEGGINGS, 6);
        p_266652_.put(ArmorItem.Type.CHESTPLATE, 6);
        p_266652_.put(ArmorItem.Type.HELMET, 3);
    }), 15, SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.1F, () -> Ingredient.of(ModItems.TITANIUM_INGOT.get())),
    
    MK3("mk3", 100, Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266652_) -> {
        p_266652_.put(ArmorItem.Type.BOOTS, 5);
        p_266652_.put(ArmorItem.Type.LEGGINGS, 8);
        p_266652_.put(ArmorItem.Type.CHESTPLATE, 10);
        p_266652_.put(ArmorItem.Type.HELMET, 5);
    }), 20, SoundEvents.ARMOR_EQUIP_GOLD, 4.0F, 0.2F, () -> Ingredient.of(ModItems.GOLD_TITANIUM_ALLOY.get()));

    public static final StringRepresentable.EnumCodec<ArmorMaterials> CODEC = StringRepresentable.fromEnum(ArmorMaterials::values);
    private static final EnumMap<ArmorItem.Type, Integer> HEALTH_FUNCTION_FOR_TYPE = Util.make(new EnumMap<>(ArmorItem.Type.class), (p_266653_) -> {
        p_266653_.put(ArmorItem.Type.BOOTS, 13);
        p_266653_.put(ArmorItem.Type.LEGGINGS, 15);
        p_266653_.put(ArmorItem.Type.CHESTPLATE, 16);
        p_266653_.put(ArmorItem.Type.HELMET, 11);
    });
    private final String name;
    private final int durabilityMultiplier;
    private final EnumMap<ArmorItem.Type, Integer> protectionFunctionForType;
    private final int enchantmentValue;
    private final SoundEvent sound;
    private final float toughness;
    private final float knockbackResistance;
    private final Supplier<Ingredient> repairIngredient;

    ModArmorMaterials(String name, int durabilityMultiplier, EnumMap<ArmorItem.Type, Integer> protectionFunctionForType, int enchantmentValue, SoundEvent sound, float toughness, float knockbackResistance, Supplier<Ingredient> repairIngredient) {
        this.name = name;
        this.durabilityMultiplier = durabilityMultiplier;
        this.protectionFunctionForType = protectionFunctionForType;
        this.enchantmentValue = enchantmentValue;
        this.sound = sound;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.repairIngredient = repairIngredient;
    }

    public int getDurabilityForType(ArmorItem.Type pType) {
        return HEALTH_FUNCTION_FOR_TYPE.get(pType) * this.durabilityMultiplier;
    }

    public int getDefenseForType(ArmorItem.Type pType) {
        return this.protectionFunctionForType.get(pType);
    }

    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    public SoundEvent getEquipSound() {
        return this.sound;
    }

    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    public String getName() {
        return IronCore.MODID + ":" + this.name;
    }

    public float getToughness() {
        return this.toughness;
    }

    public float getKnockbackResistance() {
        return this.knockbackResistance;
    }
}
