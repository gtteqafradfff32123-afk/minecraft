package com.example.titanforge.client.renderer;

import com.example.titanforge.TitanForge;
import com.example.titanforge.client.model.GraveglassColossusModel;
import com.example.titanforge.entities.GraveglassColossusEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.ResourceLocation;

public final class GraveglassColossusRenderer
    extends MobRenderer<GraveglassColossusEntity, GraveglassColossusModel> {

    private static final ResourceLocation TEXTURE = new ResourceLocation(
        TitanForge.MOD_ID, "textures/entity/graveglass_colossus.png");

    public GraveglassColossusRenderer(EntityRendererManager manager) {
        super(manager, new GraveglassColossusModel(), 0.9F);
    }

    @Override
    protected void preRenderCallback(GraveglassColossusEntity entity,
                                      MatrixStack stack, float partialTick) {
        stack.scale(1.18F, 1.18F, 1.18F);
    }

    @Override
    public ResourceLocation getEntityTexture(GraveglassColossusEntity entity) {
        return TEXTURE;
    }
}
