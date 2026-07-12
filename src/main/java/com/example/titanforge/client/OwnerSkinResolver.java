package com.example.titanforge.client;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@OnlyIn(Dist.CLIENT)
public final class OwnerSkinResolver {
    public static final class SkinData {
        public final ResourceLocation texture;
        public final boolean slim;
        public final boolean resolved;

        private SkinData(ResourceLocation texture,
                         boolean slim,
                         boolean resolved) {
            this.texture = texture;
            this.slim = slim;
            this.resolved = resolved;
        }
    }

    private static final Map<UUID, SkinData> CACHE =
        new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> REQUESTED =
        new ConcurrentHashMap<>();

    private OwnerSkinResolver() {}

    public static SkinData resolve(UUID ownerId, GameProfile fallbackProfile) {
        Minecraft mc = Minecraft.getInstance();

        if (mc.player != null
            && ownerId.equals(mc.player.getUniqueID())) {
            boolean slim = "slim".equals(mc.player.getSkinType());
            SkinData result = new SkinData(
                mc.player.getLocationSkin(), slim, true);
            CACHE.put(ownerId, result);
            return result;
        }

        if (mc.getConnection() != null) {
            NetworkPlayerInfo info =
                mc.getConnection().getPlayerInfo(ownerId);
            if (info != null) {
                boolean slim = "slim".equals(info.getSkinType());
                SkinData result = new SkinData(
                    info.getLocationSkin(), slim, true);
                CACHE.put(ownerId, result);
                return result;
            }
        }

        SkinData cached = CACHE.get(ownerId);
        if (cached != null) return cached;

        if (fallbackProfile != null
            && REQUESTED.putIfAbsent(ownerId, Boolean.TRUE) == null) {
            mc.getSkinManager().loadProfileTextures(
                fallbackProfile,
                (type, location, texture) -> {
                    if (type != MinecraftProfileTexture.Type.SKIN) return;
                    String model = texture.getMetadata("model");
                    boolean slim = "slim".equals(model);
                    CACHE.put(ownerId,
                        new SkinData(location, slim, true));
                },
                true);
        }

        return new SkinData(
            DefaultPlayerSkin.getDefaultSkin(ownerId),
            DefaultPlayerSkin.getSkinType(ownerId).equals("slim"),
            false);
    }

    public static void clear() {
        CACHE.clear();
        REQUESTED.clear();
    }
}
