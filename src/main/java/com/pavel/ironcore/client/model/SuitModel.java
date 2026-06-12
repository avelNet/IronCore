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
        return new ResourceLocation(IronCore.MODID, "textures/armor/" + animatable.getTierId() + ".png");
    }

    @Override
    public ResourceLocation getAnimationResource(BaseSuitItem animatable) {
        // Все анимации теперь хранятся в общем файле suit.animation.json для удобства
        return new ResourceLocation(IronCore.MODID, "animations/suit.animation.json");
    }
}
