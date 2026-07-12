package com.example.titanforge.client.model;

import com.example.titanforge.entities.GraveglassColossusEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.util.math.MathHelper;

public final class GraveglassColossusModel extends EntityModel<GraveglassColossusEntity> {
    private final ModelRenderer root;
    private final ModelRenderer pelvis;
    private final ModelRenderer torso;
    private final ModelRenderer head;
    private final ModelRenderer jaw;
    private final ModelRenderer rightArm;
    private final ModelRenderer rightForearm;
    private final ModelRenderer leftArm;
    private final ModelRenderer leftForearm;
    private final ModelRenderer rightLeg;
    private final ModelRenderer leftLeg;
    private final ModelRenderer core;

    public GraveglassColossusModel() {
        textureWidth = 128;
        textureHeight = 128;

        root = new ModelRenderer(this);
        root.setRotationPoint(0.0F, 24.0F, 0.0F);

        pelvis = part(root, 0, 0, -6.0F, -17.0F, -4.0F, 12, 6, 8, 0, 0, 0);
        part(pelvis, 40, 0, -7.0F, -18.0F, -3.0F, 14, 3, 6, 0, 0, 0);

        torso = new ModelRenderer(this);
        torso.setRotationPoint(0.0F, -17.0F, 0.0F);
        root.addChild(torso);
        torso.setTextureOffset(0, 16).addBox(-7.0F, -13.0F, -4.5F, 14, 13, 9, 0.0F);
        torso.setTextureOffset(48, 14).addBox(-8.5F, -11.5F, -5.0F, 17, 4, 10, 0.0F);
        torso.setTextureOffset(0, 40).addBox(-6.0F, -8.0F, -5.6F, 12, 2, 2, 0.0F);
        torso.setTextureOffset(0, 44).addBox(-5.5F, -4.5F, -5.6F, 11, 2, 2, 0.0F);

        core = new ModelRenderer(this);
        core.setRotationPoint(0.0F, -6.0F, -5.0F);
        torso.addChild(core);
        core.setTextureOffset(30, 40).addBox(-2.5F, -2.5F, -1.5F, 5, 5, 3, 0.0F);
        core.rotateAngleZ = 0.7853982F;

        head = new ModelRenderer(this);
        head.setRotationPoint(0.0F, -30.0F, 0.0F);
        root.addChild(head);
        head.setTextureOffset(0, 50).addBox(-5.0F, -7.0F, -4.5F, 10, 8, 9, 0.0F);
        head.setTextureOffset(38, 50).addBox(-4.0F, -4.0F, -5.6F, 3, 2, 2, 0.0F);
        head.setTextureOffset(48, 50).addBox(1.0F, -4.0F, -5.6F, 3, 2, 2, 0.0F);

        jaw = new ModelRenderer(this);
        jaw.setRotationPoint(0.0F, 0.0F, -3.0F);
        head.addChild(jaw);
        jaw.setTextureOffset(38, 56).addBox(-4.5F, -1.0F, -2.0F, 9, 4, 5, 0.0F);

        shard(head, -4.0F, -8.0F, 0.0F, -0.28F);
        shard(head, 0.0F, -10.0F, 0.5F, 0.0F);
        shard(head, 4.0F, -8.0F, 0.0F, 0.28F);

        rightArm = limb(torso, 64, 32, -10.0F, -10.0F, 0.0F, -3.5F, -1.0F, -3.5F, 7, 13, 7);
        rightForearm = limb(rightArm, 92, 32, 0.0F, 11.0F, 0.0F, -3.0F, 0.0F, -3.0F, 6, 13, 6);
        claw(rightForearm, -2.0F, 12.0F, -2.0F);

        leftArm = limb(torso, 64, 58, 10.0F, -10.0F, 0.0F, -3.5F, -1.0F, -3.5F, 7, 12, 7);
        leftForearm = limb(leftArm, 92, 58, 0.0F, 10.0F, 0.0F, -3.0F, 0.0F, -3.0F, 6, 12, 6);
        claw(leftForearm, -2.0F, 11.0F, -2.0F);

        rightLeg = limb(pelvis, 0, 72, -3.7F, -11.0F, 0.0F, -3.5F, 0.0F, -3.5F, 7, 13, 7);
        part(rightLeg, 30, 72, -4.0F, 11.0F, -6.0F, 8, 3, 10, 0, 0, 0);
        leftLeg = limb(pelvis, 0, 92, 3.7F, -11.0F, 0.0F, -3.5F, 0.0F, -3.5F, 7, 13, 7);
        part(leftLeg, 40, 92, -4.0F, 11.0F, -6.0F, 8, 3, 10, 0, 0, 0);

        crystal(torso, -5.0F, -11.0F, 4.0F, -0.35F);
        crystal(torso, 0.0F, -9.0F, 4.2F, 0.0F);
        crystal(torso, 5.0F, -6.0F, 4.0F, 0.35F);
    }

    private ModelRenderer part(ModelRenderer parent, int u, int v, float x, float y, float z,
                               int w, int h, int d, float px, float py, float pz) {
        ModelRenderer p = new ModelRenderer(this);
        p.setRotationPoint(px, py, pz);
        p.setTextureOffset(u, v).addBox(x, y, z, w, h, d, 0.0F);
        parent.addChild(p);
        return p;
    }

    private ModelRenderer limb(ModelRenderer parent, int u, int v, float px, float py, float pz,
                               float x, float y, float z, int w, int h, int d) {
        return part(parent, u, v, x, y, z, w, h, d, px, py, pz);
    }

    private void shard(ModelRenderer parent, float x, float y, float z, float rotZ) {
        ModelRenderer s = part(parent, 84, 0, -1.0F, -5.0F, -1.0F, 2, 6, 2, x, y, z);
        s.rotateAngleZ = rotZ;
    }

    private void crystal(ModelRenderer parent, float x, float y, float z, float rotZ) {
        ModelRenderer c = part(parent, 96, 0, -1.5F, -6.0F, -1.5F, 3, 7, 3, x, y, z);
        c.rotateAngleZ = rotZ;
        c.rotateAngleX = -0.25F;
    }

    private void claw(ModelRenderer parent, float x, float y, float z) {
        part(parent, 108, 0, 0, 0, 0, 1, 6, 1, x, y, z);
        part(parent, 112, 0, 0, 0, 0, 1, 7, 1, x + 2.0F, y, z - 0.5F);
        part(parent, 116, 0, 0, 0, 0, 1, 5, 1, x + 4.0F, y, z);
    }

    @Override
    public void setRotationAngles(GraveglassColossusEntity entity, float limbSwing,
                                   float limbSwingAmount, float age, float yaw, float pitch) {
        head.rotateAngleY = yaw * ((float)Math.PI / 180F);
        head.rotateAngleX = pitch * ((float)Math.PI / 180F);
        jaw.rotateAngleX = 0.08F + MathHelper.sin(age * 0.09F) * 0.035F;

        float walk = MathHelper.cos(limbSwing * 0.55F) * 0.85F * limbSwingAmount;
        rightLeg.rotateAngleX = walk;
        leftLeg.rotateAngleX = -walk;
        rightArm.rotateAngleX = -walk * 0.72F;
        leftArm.rotateAngleX = walk * 0.72F;
        rightArm.rotateAngleZ = 0.08F;
        leftArm.rotateAngleZ = -0.08F;

        float breathe = MathHelper.sin(age * 0.07F) * 0.025F;
        torso.rotateAngleZ = breathe;
        core.rotateAngleZ = 0.7853982F + age * 0.01F;

        if (entity.isAggressive()) {
            float swing = MathHelper.sin(this.swingProgress * (float)Math.PI);
            float recovery = MathHelper.sin(
                (1.0F - (1.0F - this.swingProgress) *
                (1.0F - this.swingProgress)) * (float)Math.PI
            );

            rightArm.rotateAngleX = -1.05F - swing * 1.15F;
            leftArm.rotateAngleX = -0.72F - recovery * 0.35F;
            rightForearm.rotateAngleX = -0.18F - swing * 0.55F;
            jaw.rotateAngleX = 0.24F + swing * 0.22F;
        }
    }

    @Override
    public void render(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight,
                       int packedOverlay, float red, float green, float blue, float alpha) {
        root.render(matrixStack, buffer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
