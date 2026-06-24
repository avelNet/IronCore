package com.pavel.ironcore.data;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.block.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModBlockTagsProvider extends BlockTagsProvider {
    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, IronCore.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.TITANIUM_ORE.get(),
                        ModBlocks.DEEPSLATE_TITANIUM_ORE.get(),
                        ModBlocks.PALLADIUM_ORE.get(),
                        ModBlocks.ALLOY_SMELTER.get(),
                        ModBlocks.COAL_GENERATOR.get(),
                        ModBlocks.ASSEMBLY_TABLE.get(),
                        ModBlocks.SUIT_STATION.get(),
                        ModBlocks.ARC_REACTOR_CORE.get());

        // Титан копается каменной киркой (Iron level in vanilla terms, but stone is base for iron ore)
        this.tag(BlockTags.NEEDS_STONE_TOOL)
                .add(ModBlocks.TITANIUM_ORE.get(),
                        ModBlocks.DEEPSLATE_TITANIUM_ORE.get());

        // Палладий копается железной киркой (Diamond level for rare ores like gold/diamond)
        this.tag(BlockTags.NEEDS_IRON_TOOL)
                .add(ModBlocks.PALLADIUM_ORE.get());
    }
}
