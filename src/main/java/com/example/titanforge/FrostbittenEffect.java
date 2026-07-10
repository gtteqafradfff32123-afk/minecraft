package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.SoundEvents;

public class FrostbittenEffect extends Effect {
    public FrostbittenEffect() {
        super(EffectType.HARMFUL, 0x88CCFF);
    }

    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        entity.attackEntityFrom(ModDamageSources.FROSTBITE, 0.5F * (amplifier + 1));
        if (!entity.world.isRemote) {
            ((net.minecraft.world.server.ServerWorld)entity.world).spawnParticle(
                ParticleTypes.ITEM_SNOWBALL, entity.getPosX(), entity.getPosY() + 1.0, entity.getPosZ(),
                8, 0.3, 0.5, 0.3, 0.0);
            entity.playSound(SoundEvents.BLOCK_GLASS_BREAK, 0.4F, 1.5F);
        }
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return duration % 20 == 0;
    }
}
