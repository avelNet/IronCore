package com.pavel.ironcore.data;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, IronCore.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simpleItem(ModItems.RAW_TITANIUM);
        simpleItem(ModItems.TITANIUM_INGOT);
        simpleItem(ModItems.RAW_PALLADIUM);
        simpleItem(ModItems.PALLADIUM_INGOT);
        simpleItem(ModItems.GOLD_TITANIUM_ALLOY);
        simpleItem(ModItems.PALLADIUM_REACTOR);
        simpleItem(ModItems.CHLOROPHYLL_JUICE);
        
        // Items with custom textures that don't need complex models right now
        simpleItem(ModItems.COAL_REACTOR);
        simpleItem(ModItems.MK1_HELMET);
        simpleItem(ModItems.MK1_CHESTPLATE);
        simpleItem(ModItems.MK1_LEGGINGS);
        simpleItem(ModItems.MK1_BOOTS);
        simpleItem(ModItems.MK2_HELMET);
        simpleItem(ModItems.MK2_CHESTPLATE);
        simpleItem(ModItems.MK2_LEGGINGS);
        simpleItem(ModItems.MK2_BOOTS);
    }

    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/generated")).texture("layer0",
                new ResourceLocation(IronCore.MODID,"item/" + item.getId().getPath()));
    }
}
