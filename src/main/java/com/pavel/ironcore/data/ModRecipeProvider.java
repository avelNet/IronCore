package com.pavel.ironcore.data;

import com.pavel.ironcore.block.ModBlocks;
import com.pavel.ironcore.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        // --- Переплавка руды в слитки (Ванильная печь) ---
        oreSmelting(pWriter, List.of(ModItems.RAW_TITANIUM.get()), RecipeCategory.MISC, ModItems.TITANIUM_INGOT.get(), 0.7f, 200, "titanium");
        oreSmelting(pWriter, List.of(ModItems.RAW_PALLADIUM.get()), RecipeCategory.MISC, ModItems.PALLADIUM_INGOT.get(), 1.0f, 200, "palladium");
        
        oreBlasting(pWriter, List.of(ModItems.RAW_TITANIUM.get()), RecipeCategory.MISC, ModItems.TITANIUM_INGOT.get(), 0.7f, 100, "titanium");
        oreBlasting(pWriter, List.of(ModItems.RAW_PALLADIUM.get()), RecipeCategory.MISC, ModItems.PALLADIUM_INGOT.get(), 1.0f, 100, "palladium");

        // --- Рецепты механизмов (Ванильный верстак) ---
        
        // Угольный генератор
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.COAL_GENERATOR.get())
                .pattern("III")
                .pattern("IFI")
                .pattern("III")
                .define('I', Items.IRON_INGOT)
                .define('F', Items.FURNACE)
                .unlockedBy("has_furnace", has(Items.FURNACE))
                .save(pWriter);

        // Сплав-печь
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ALLOY_SMELTER.get())
                .pattern("CIC")
                .pattern("IBI")
                .pattern("CIC")
                .define('I', Items.IRON_INGOT)
                .define('C', Items.COPPER_INGOT)
                .define('B', Items.BLAST_FURNACE)
                .unlockedBy("has_blast_furnace", has(Items.BLAST_FURNACE))
                .save(pWriter);

        // Сборочный стол
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ASSEMBLY_TABLE.get())
                .pattern("IBI")
                .pattern("SWS")
                .pattern("SSS")
                .define('I', Items.IRON_BLOCK)
                .define('B', Items.IRON_BARS)
                .define('S', Items.SMOOTH_STONE)
                .define('W', Items.CRAFTING_TABLE)
                .unlockedBy("has_crafting_table", has(Items.CRAFTING_TABLE))
                .save(pWriter);

                // Сборочный стенд (Suit Station)
                ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SUIT_STATION.get())
                .pattern(" B ")
                .pattern("BHB")
                .pattern("SSS")
                .define('B', Items.IRON_BLOCK)
                .define('H', Items.HOPPER)
                .define('S', Items.SMOOTH_STONE)
                .unlockedBy("has_iron_block", has(Items.IRON_BLOCK))
                .save(pWriter);

                // Хлорофилловый сок
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.CHARGING_STATION.get())
                .pattern("CRC")
                .pattern("RIR")
                .pattern("CRC")
                .define('I', Items.IRON_BLOCK)
                .define('C', Items.COPPER_INGOT)
                .define('R', Items.REDSTONE)
                .unlockedBy("has_iron_block", has(Items.IRON_BLOCK))
                .save(pWriter);

        // Хлорофилловый сок (Простой крафт: Пузырек + любая листва)
        ShapelessRecipeBuilder.shapeless(RecipeCategory.FOOD, ModItems.CHLOROPHYLL_JUICE.get())
                .requires(Items.GLASS_BOTTLE)
                .requires(net.minecraft.tags.ItemTags.LEAVES)
                .unlockedBy("has_palladium", has(ModItems.RAW_PALLADIUM.get()))
                .save(pWriter);
    }

    protected static void oreSmelting(Consumer<FinishedRecipe> pFinishedRecipeConsumer, List<net.minecraft.world.level.ItemLike> pIngredients, RecipeCategory pCategory, net.minecraft.world.level.ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pFinishedRecipeConsumer, RecipeSerializer.SMELTING_RECIPE, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(Consumer<FinishedRecipe> pFinishedRecipeConsumer, List<net.minecraft.world.level.ItemLike> pIngredients, RecipeCategory pCategory, net.minecraft.world.level.ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pFinishedRecipeConsumer, RecipeSerializer.BLASTING_RECIPE, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static void oreCooking(Consumer<FinishedRecipe> pFinishedRecipeConsumer, RecipeSerializer<? extends AbstractCookingRecipe> pCookingSerializer, List<net.minecraft.world.level.ItemLike> pIngredients, RecipeCategory pCategory, net.minecraft.world.level.ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(net.minecraft.world.level.ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult, pExperience, pCookingTime, pCookingSerializer)
                    .group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(pFinishedRecipeConsumer, com.pavel.ironcore.IronCore.MODID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}
