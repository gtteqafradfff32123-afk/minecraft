package com.example.titanforge.client;

import com.example.titanforge.entities.ShadowEntity;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.model.PlayerModel;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class ShadowRenderer
        extends MobRenderer<ShadowEntity, PlayerModel<ShadowEntity>> {

    private static final Map<UUID, ResourceLocation> SKINS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> REQUESTED = new ConcurrentHashMap<>();

    public ShadowRenderer(EntityRendererManager manager) {
        super(manager, new PlayerModel<>(0.0F, false), 0.5F);
    }

    @Override
    public ResourceLocation getEntityTexture(ShadowEntity entity) {
        UUID owner = entity.getOwnerId().orElse(entity.getUniqueID());
        Minecraft mc = Minecraft.getInstance();

        if (mc.getConnection() != null) {
            NetworkPlayerInfo info = mc.getConnection().getPlayerInfo(owner);
            if (info != null) {
                ResourceLocation skin = info.getLocationSkin();
                SKINS.put(owner, skin);
                return skin;
            }
        }

        ResourceLocation cached = SKINS.get(owner);
        if (cached != null) return cached;

        if (REQUESTED.putIfAbsent(owner, Boolean.TRUE) == null) {
            GameProfile profile = entity.getOwnerProfile();
            if (profile != null) {
                mc.getSkinManager().loadProfileTextures(profile, (type, location, texture) -> {
                    if (type == MinecraftProfileTexture.Type.SKIN) {
                        SKINS.put(owner, location);
                    }
                }, true);
            }
        }

        return DefaultPlayerSkin.getDefaultSkin(owner);
    }
}
