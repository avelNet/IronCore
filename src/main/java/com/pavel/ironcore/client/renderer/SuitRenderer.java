package com.pavel.ironcore.client.renderer;

import com.pavel.ironcore.client.model.SuitModel;
import com.pavel.ironcore.item.BaseSuitItem;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import software.bernie.geckolib.cache.object.GeoBone;

public class SuitRenderer extends GeoArmorRenderer<BaseSuitItem> {
    public SuitRenderer() {
        super(new SuitModel());
    }

    @Override
    public GeoBone getHeadBone() {
        return this.model.getBone("armorHead").orElse(this.model.getBone("bipedHead").orElse(null));
    }

    @Override
    public GeoBone getBodyBone() {
        return this.model.getBone("armorBody").orElse(this.model.getBone("bipedBody").orElse(null));
    }

    @Override
    public GeoBone getRightArmBone() {
        return this.model.getBone("armorRightArm").orElse(this.model.getBone("bipedRightArm").orElse(null));
    }

    @Override
    public GeoBone getLeftArmBone() {
        return this.model.getBone("armorLeftArm").orElse(this.model.getBone("bipedLeftArm").orElse(null));
    }

    @Override
    public GeoBone getRightLegBone() {
        return this.model.getBone("armorRightLeg").orElse(this.model.getBone("bipedRightLeg").orElse(null));
    }

    @Override
    public GeoBone getLeftLegBone() {
        return this.model.getBone("armorLeftLeg").orElse(this.model.getBone("bipedLeftLeg").orElse(null));
    }

    @Override
    public GeoBone getRightBootBone() {
        return this.model.getBone("armorRightBoot").orElse(this.model.getBone("bipedRightBoot").orElse(this.model.getBone("bipedRightLeg").orElse(null)));
    }

    @Override
    public GeoBone getLeftBootBone() {
        return this.model.getBone("armorLeftBoot").orElse(this.model.getBone("bipedLeftBoot").orElse(this.model.getBone("bipedLeftLeg").orElse(null)));
    }
}
