package com.example.titanforge.client;

import com.example.titanforge.liminal.LiminalDimension;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.ZombieRenderer;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.util.ResourceLocation;

public class LiminalZombieRenderer extends ZombieRenderer {
    private static final ResourceLocation LIMINAL_ZOMBIE =
        new ResourceLocation("titanforge", "textures/entity/liminal_zombie.png");

    public LiminalZombieRenderer(EntityRendererManager mgr) {
        super(mgr);
    }

    @Override
    public ResourceLocation getEntityTexture(ZombieEntity entity) {
        if (entity.world != null && entity.world.getDimensionKey() == LiminalDimension.LIMINAL_WORLD) {
            return LIMINAL_ZOMBIE;
        }
        return super.getEntityTexture(entity);
    }
}
