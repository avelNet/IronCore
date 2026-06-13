package com.pavel.ironcore.client.model;

import com.pavel.ironcore.IronCore;
import com.pavel.ironcore.item.BaseSuitItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class SuitModel extends GeoModel<BaseSuitItem> {
    @Override
    public ResourceLocation getModelResource(BaseSuitItem animatable) {
        return new ResourceLocation(IronCore.MODID, "geo/" + animatable.getTierId() + ".geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BaseSuitItem animatable) {
        String tier = animatable.getTierId();
        return new ResourceLocation(IronCore.MODID, "textures/armor/" + tier + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(BaseSuitItem animatable) {
        return new ResourceLocation(IronCore.MODID, "animations/" + animatable.getTierId() + ".animation.json");
    }
}
