package com.pavel.ironcore.data;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, IronCore.MODID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.TITANIUM_ORE);
        blockWithItem(ModBlocks.DEEPSLATE_TITANIUM_ORE);
        blockWithItem(ModBlocks.PALLADIUM_ORE);
        simpleBlockWithItem(ModBlocks.ALLOY_SMELTER.get(), models().cubeBottomTop("alloy_smelter", 
                modLoc("block/alloy_smelter_side"), 
                modLoc("block/alloy_smelter_bottom"), 
                modLoc("block/alloy_smelter_top")));
        simpleBlockWithItem(ModBlocks.COAL_GENERATOR.get(), cubeAll(ModBlocks.COAL_GENERATOR.get()));
        simpleBlockWithItem(ModBlocks.ASSEMBLY_TABLE.get(), cubeAll(ModBlocks.ASSEMBLY_TABLE.get()));
        simpleBlockWithItem(ModBlocks.CHARGING_STATION.get(), cubeAll(ModBlocks.CHARGING_STATION.get()));
        simpleBlockWithItem(ModBlocks.SUIT_STATION.get(), cubeAll(ModBlocks.SUIT_STATION.get()));
        simpleBlockWithItem(ModBlocks.ARC_REACTOR_CORE.get(), cubeAll(ModBlocks.ARC_REACTOR_CORE.get()));
    }

    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }
}
