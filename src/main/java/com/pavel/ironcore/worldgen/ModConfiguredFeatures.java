package com.pavel.ironcore.worldgen;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.data.worldgen.features.FeatureUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;

import java.util.List;

public class ModConfiguredFeatures {
    public static final ResourceKey<ConfiguredFeature<?, ?>> TITANIUM_ORE_KEY = registerKey("titanium_ore");
    public static final ResourceKey<ConfiguredFeature<?, ?>> PALLADIUM_ORE_KEY = registerKey("palladium_ore");

    public static void bootstrap(BootstapContext<ConfiguredFeature<?, ?>> context) {
        RuleTest stoneReplaceables = new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES);
        RuleTest deepslateReplaceables = new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES);

        List<OreConfiguration.TargetBlockState> titaniumOres = List.of(
                OreConfiguration.target(stoneReplaceables, ModBlocks.TITANIUM_ORE.get().defaultBlockState()),
                OreConfiguration.target(deepslateReplaceables, ModBlocks.DEEPSLATE_TITANIUM_ORE.get().defaultBlockState()));

        FeatureUtils.register(context, TITANIUM_ORE_KEY, Feature.ORE, new OreConfiguration(titaniumOres, 6)); // Размер жилы
        
        FeatureUtils.register(context, PALLADIUM_ORE_KEY, Feature.ORE, new OreConfiguration(deepslateReplaceables, ModBlocks.PALLADIUM_ORE.get().defaultBlockState(), 4)); // Размер жилы палладия
    }

    public static ResourceKey<ConfiguredFeature<?, ?>> registerKey(String name) {
        return ResourceKey.create(Registries.CONFIGURED_FEATURE, new ResourceLocation(IronCore.MODID, name));
    }
}
