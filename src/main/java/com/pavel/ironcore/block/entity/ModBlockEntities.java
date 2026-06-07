package com.pavel.ironcore.block.entity;

import com.pavel.ironcore.IronCore;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, IronCore.MODID);

    public static final RegistryObject<BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER_BE =
            BLOCK_ENTITIES.register("alloy_smelter", () ->
                    BlockEntityType.Builder.of(AlloySmelterBlockEntity::new,
                            com.pavel.ironcore.block.ModBlocks.ALLOY_SMELTER.get()).build(null));

    public static final RegistryObject<BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR_BE =
            BLOCK_ENTITIES.register("coal_generator", () ->
                    BlockEntityType.Builder.of(CoalGeneratorBlockEntity::new,
                            com.pavel.ironcore.block.ModBlocks.COAL_GENERATOR.get()).build(null));

    public static final RegistryObject<BlockEntityType<AssemblyTableBlockEntity>> ASSEMBLY_TABLE_BE =
            BLOCK_ENTITIES.register("assembly_table", () ->
                    BlockEntityType.Builder.of(AssemblyTableBlockEntity::new,
                            com.pavel.ironcore.block.ModBlocks.ASSEMBLY_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<ChargingStationBlockEntity>> CHARGING_STATION_BE =
            BLOCK_ENTITIES.register("charging_station", () ->
                    BlockEntityType.Builder.of(ChargingStationBlockEntity::new,
                            com.pavel.ironcore.block.ModBlocks.CHARGING_STATION.get()).build(null));

    public static final RegistryObject<BlockEntityType<SuitStationBlockEntity>> SUIT_STATION_BE =
            BLOCK_ENTITIES.register("suit_station", () ->
                    BlockEntityType.Builder.of(SuitStationBlockEntity::new,
                            com.pavel.ironcore.block.ModBlocks.SUIT_STATION.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
