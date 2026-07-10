package com.example.titanforge.client;

import com.example.titanforge.entities.ShadowEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ShadowRenderer extends MobRenderer<ShadowEntity, PlayerModel<ShadowEntity>> {

    public ShadowRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getEntityTexture(ShadowEntity entity) {
        UUID ownerId = entity.getOwnerId().orElse(entity.getUniqueID());
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(ownerId);
            if (info != null)
                return info.getLocationSkin();
        }
        return DefaultPlayerSkin.getDefaultSkin(ownerId);
    }
}
