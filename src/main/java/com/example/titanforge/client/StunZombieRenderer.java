package com.example.titanforge.client;

import com.example.titanforge.TitanForge;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.util.ResourceLocation;

public class StunZombieRenderer extends ZombieRenderer {
    private static final ResourceLocation TEXTURE =
        new ResourceLocation(TitanForge.MOD_ID, "textures/entity/stun_zombie.png");

    public StunZombieRenderer(EntityRendererManager manager) {
        super(manager);
    }

    @Override
    public ResourceLocation getEntityTexture(ZombieEntity entity) {
        return TEXTURE;
    }
}
