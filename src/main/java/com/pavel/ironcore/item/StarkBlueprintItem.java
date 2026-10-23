package com.pavel.ironcore.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.SmithingTemplateItem;

import java.util.List;

/** Шаблон кузнечного стола для апгрейда костюма Mk1 → Mk2 (титан) → Mk3 (сплав). */
public class StarkBlueprintItem extends SmithingTemplateItem {
    private static final List<ResourceLocation> ARMOR_ICONS = List.of(
            new ResourceLocation("item/empty_armor_slot_helmet"),
            new ResourceLocation("item/empty_armor_slot_chestplate"),
            new ResourceLocation("item/empty_armor_slot_leggings"),
            new ResourceLocation("item/empty_armor_slot_boots"));
    private static final List<ResourceLocation> INGOT_ICONS = List.of(new ResourceLocation("item/empty_slot_ingot"));

    public StarkBlueprintItem() {
        super(
                Component.translatable("item.ironcore.stark_blueprint.applies_to").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.ironcore.stark_blueprint.ingredients").withStyle(ChatFormatting.BLUE),
                Component.translatable("item.ironcore.stark_blueprint.upgrade").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.ironcore.stark_blueprint.base_slot"),
                Component.translatable("item.ironcore.stark_blueprint.additions_slot"),
                ARMOR_ICONS,
                INGOT_ICONS);
    }

    @Override
    public String getDescriptionId() {
        return "item.ironcore.stark_blueprint";
    }
}
