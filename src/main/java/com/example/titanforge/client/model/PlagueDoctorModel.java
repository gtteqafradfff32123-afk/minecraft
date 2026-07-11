package com.example.titanforge.client.model;

import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.MobEntity;

public final class PlagueDoctorModel<T extends MobEntity> extends BipedModel<T> {

    private final ModelRenderer beak;
    private final ModelRenderer hat;
    private final ModelRenderer collar;
    private final ModelRenderer coatSkirt;
    private final ModelRenderer bottles;

    public PlagueDoctorModel() {
        this(0.0F);
    }

    public PlagueDoctorModel(float modelSize) {
        super(modelSize, 0.0F, 64, 64);

        this.beak = new ModelRenderer(this);
        this.beak.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.bipedHead.addChild(this.beak);
        this.beak.setTextureOffset(0, 32)
                .addBox(-2.5F, -5.5F, -8.0F, 5.0F, 4.0F, 4.0F, modelSize);
        this.beak.setTextureOffset(0, 40)
                .addBox(-2.0F, -5.0F, -11.0F, 4.0F, 3.0F, 3.0F, modelSize);
        this.beak.setTextureOffset(0, 46)
                .addBox(-1.0F, -4.5F, -13.0F, 2.0F, 2.0F, 2.0F, modelSize);
        setRotation(this.beak, -0.08726646F, 0.0F, 0.0F);

        this.hat = new ModelRenderer(this);
        this.hat.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.bipedHead.addChild(this.hat);
        this.hat.setTextureOffset(0, 48)
                .addBox(-7.0F, -9.0F, -7.0F, 14.0F, 1.0F, 14.0F, modelSize);
        this.hat.setTextureOffset(28, 32)
                .addBox(-4.5F, -13.0F, -4.5F, 9.0F, 4.0F, 9.0F, modelSize);
        this.hat.setTextureOffset(32, 20)
                .addBox(-3.5F, -15.0F, -3.5F, 7.0F, 2.0F, 7.0F, modelSize);

        this.collar = new ModelRenderer(this);
        this.collar.setRotationPoint(0.0F, 0.0F, 0.0F);
        this.bipedBody.addChild(this.collar);
        this.collar.setTextureOffset(16, 32)
                .addBox(-5.0F, -0.5F, -2.5F, 10.0F, 3.0F, 5.0F, modelSize + 0.10F);

        this.coatSkirt = new ModelRenderer(this);
        this.coatSkirt.setRotationPoint(0.0F, 10.0F, 0.0F);
        this.bipedBody.addChild(this.coatSkirt);
        this.coatSkirt.setTextureOffset(16, 40)
                .addBox(-4.5F, 0.0F, -2.25F, 9.0F, 6.0F, 4.5F, modelSize + 0.10F);

        this.bottles = new ModelRenderer(this);
        this.bottles.setRotationPoint(0.0F, 9.0F, 0.0F);
        this.bipedBody.addChild(this.bottles);
        this.bottles.setTextureOffset(56, 22)
                .addBox(-5.5F, 0.0F, -3.0F, 2.0F, 3.0F, 2.0F, modelSize);
        this.bottles.setTextureOffset(60, 22)
                .addBox(-5.0F, -1.0F, -2.5F, 1.0F, 1.0F, 1.0F, modelSize);
        this.bottles.setTextureOffset(56, 27)
                .addBox(3.5F, 0.5F, -3.0F, 2.0F, 2.5F, 2.0F, modelSize);
        this.bottles.setTextureOffset(60, 27)
                .addBox(4.0F, -0.5F, -2.5F, 1.0F, 1.0F, 1.0F, modelSize);

        this.bipedRightArm.setTextureOffset(40, 44)
                .addBox(-3.15F, 7.0F, -2.15F, 4.3F, 5.0F, 4.3F, modelSize + 0.05F);
        this.bipedLeftArm.setTextureOffset(40, 44)
                .addBox(-1.15F, 7.0F, -2.15F, 4.3F, 5.0F, 4.3F, modelSize + 0.05F);
        this.bipedRightLeg.setTextureOffset(48, 9)
                .addBox(-2.15F, 8.0F, -3.0F, 4.3F, 4.0F, 5.0F, modelSize + 0.05F);
        this.bipedLeftLeg.setTextureOffset(48, 9)
                .addBox(-2.15F, 8.0F, -3.0F, 4.3F, 4.0F, 5.0F, modelSize + 0.05F);
    }

    private static void setRotation(ModelRenderer part, float x, float y, float z) {
        part.rotateAngleX = x;
        part.rotateAngleY = y;
        part.rotateAngleZ = z;
    }
}
