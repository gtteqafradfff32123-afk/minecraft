package com.example.titanforge.client;

import com.example.titanforge.entities.PlayerCopyEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class PlayerCopyRenderer extends MobRenderer<PlayerCopyEntity, PlayerModel<PlayerCopyEntity>> {

    public PlayerCopyRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getEntityTexture(PlayerCopyEntity entity) {
        GameProfile gp = entity.getOwnerProfile();
        Minecraft mc = Minecraft.getInstance();
        UUID fallbackId = entity.getOwnerId().orElse(entity.getUniqueID());
        if (gp != null) {
            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> tex =
                    mc.getSkinManager().loadSkinFromCache(gp);
            if (tex.containsKey(MinecraftProfileTexture.Type.SKIN))
                return mc.getSkinManager().loadSkin(tex.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            mc.getSkinManager().loadProfileTextures(gp, (type, location, profileTexture) -> {}, true);
        }
        return DefaultPlayerSkin.getDefaultSkin(fallbackId);
    }
}
