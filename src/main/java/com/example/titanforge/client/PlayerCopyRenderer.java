package com.example.titanforge.client;

import com.example.titanforge.entities.PlayerCopyEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.BipedArmorLayer;
import net.minecraft.client.renderer.entity.layers.HeldItemLayer;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public final class PlayerCopyRenderer
    extends MobRenderer<PlayerCopyEntity, PlayerModel<PlayerCopyEntity>> {

    private final PlayerModel<PlayerCopyEntity> wideModel =
        new PlayerModel<>(0.0F, false);
    private final PlayerModel<PlayerCopyEntity> slimModel =
        new PlayerModel<>(0.0F, true);

    public PlayerCopyRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
        this.addLayer(new BipedArmorLayer<>(
            this,
            new BipedModel<>(0.5F),
            new BipedModel<>(1.0F)));
        this.addLayer(new HeldItemLayer<>(this));
    }

    private OwnerSkinResolver.SkinData skin(PlayerCopyEntity entity) {
        UUID owner = entity.getOwnerId()
            .orElse(entity.getUniqueID());
        return OwnerSkinResolver.resolve(
            owner, entity.getOwnerProfile());
    }

    @Override
    public ResourceLocation getEntityTexture(PlayerCopyEntity entity) {
        return skin(entity).texture;
    }

    @Override
    public void render(PlayerCopyEntity entity,
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
