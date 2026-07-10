package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundCategory;

public class BloodFrenzyEffect extends Effect {
    public BloodFrenzyEffect() {
        super(EffectType.BENEFICIAL, 0x8B0000);
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }

    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        if (entity.world.isRemote) return;

        if (entity instanceof PlayerEntity && entity.ticksExisted % 80 == 0) {
            PlayerEntity player = (PlayerEntity) entity;
            player.world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(),
                    ModSounds.BLOOD_FRENZY_BREATH.get(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        }

        int speed = amplifier == 0 ? 1 : 2;
        entity.addPotionEffect(new EffectInstance(Effects.SPEED, 25, speed, false, false));

        if (amplifier >= 1) {
            entity.addPotionEffect(new EffectInstance(Effects.RESISTANCE, 25, amplifier, false, false));
        }

        if (entity.getActivePotionEffect(Effects.ABSORPTION) == null) {
            entity.addPotionEffect(new EffectInstance(Effects.ABSORPTION, 200, amplifier, false, false));
        }
    }
}
