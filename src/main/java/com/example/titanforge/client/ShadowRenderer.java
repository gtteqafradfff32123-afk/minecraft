package com.example.titanforge.client;

import com.example.titanforge.entities.ShadowEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class ShadowRenderer
    extends MobRenderer<ShadowEntity, PlayerModel<ShadowEntity>> {

    private final PlayerModel<ShadowEntity> wideModel =
        new PlayerModel<>(0.0F, false);
    private final PlayerModel<ShadowEntity> slimModel =
        new PlayerModel<>(0.0F, true);

    public ShadowRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    private OwnerSkinResolver.SkinData skin(ShadowEntity entity) {
        UUID owner = entity.getOwnerId()
            .orElse(entity.getUniqueID());
        return OwnerSkinResolver.resolve(
            owner, entity.getOwnerProfile());
    }

    @Override
    public ResourceLocation getEntityTexture(ShadowEntity entity) {
        return skin(entity).texture;
    }

    @Override
    public void render(ShadowEntity entity,
                       float entityYaw,
                       float partialTicks,
                       MatrixStack matrix,
                       IRenderTypeBuffer buffers,
                       int packedLight) {
        OwnerSkinResolver.SkinData data = skin(entity);
        this.entityModel = data.slim ? slimModel : wideModel;
        super.render(entity, entityYaw, partialTicks,
            matrix, buffers, packedLight);
    }
}
