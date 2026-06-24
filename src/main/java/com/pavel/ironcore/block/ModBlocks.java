package com.pavel.ironcore.block;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, IronCore.MODID);

    public static final RegistryObject<Block> TITANIUM_ORE = registerBlock("titanium_ore", 
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE).requiresCorrectToolForDrops().strength(5.0f, 6.0f).sound(SoundType.STONE)));
            
    public static final RegistryObject<Block> DEEPSLATE_TITANIUM_ORE = registerBlock("deepslate_titanium_ore", 
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE_IRON_ORE).requiresCorrectToolForDrops().strength(6.5f, 6.0f).sound(SoundType.DEEPSLATE)));

    public static final RegistryObject<Block> PALLADIUM_ORE = registerBlock("palladium_ore", 
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.GOLD_ORE).requiresCorrectToolForDrops().strength(4.0f, 5.0f).sound(SoundType.STONE)));

    public static final RegistryObject<Block> ALLOY_SMELTER = registerBlock("alloy_smelter", 
            () -> new AlloySmelterBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> COAL_GENERATOR = registerBlock("coal_generator", 
            () -> new CoalGeneratorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> SUIT_STATION = registerBlock("suit_station",
            () -> new SuitStationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).noOcclusion()));

    public static final RegistryObject<Block> ARC_REACTOR_CORE = registerBlock("arc_reactor_core",
            () -> new ArcReactorCoreBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).lightLevel(state -> 15).noOcclusion()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
