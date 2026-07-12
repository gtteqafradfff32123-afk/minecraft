package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;

public class LiminalEffect extends Effect {
    public LiminalEffect() {
        super(EffectType.HARMFUL, 0x2D1B69);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return duration == 1;
    }

    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        if (entity.world.isRemote) return;
        ServerWorld world = (ServerWorld) entity.world;
        world.spawnParticle(ParticleTypes.PORTAL,
            entity.getPosX(), entity.getPosY() + entity.getHeight() / 2, entity.getPosZ(),
            60, 0.4, 0.8, 0.4, 0.3);
        com.example.titanforge.liminal.LiminalManager.enter(entity, null);
    }
}
