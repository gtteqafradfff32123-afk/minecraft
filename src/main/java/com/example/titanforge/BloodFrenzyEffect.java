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

        if (entity instanceof net.minecraft.entity.player.ServerPlayerEntity
                && entity.ticksExisted % 80 == 0) {
            NetworkHandler.sendTo((net.minecraft.entity.player.ServerPlayerEntity) entity,
                    new PlayOneShotSoundPacket("blood_frenzy_breath"));
        }

        int speed = amplifier == 0 ? 0 : 1;
        entity.addPotionEffect(new EffectInstance(Effects.SPEED, 25, speed, false, false));

        if (amplifier >= 1) {
            entity.addPotionEffect(new EffectInstance(Effects.RESISTANCE, 25, amplifier - 1, false, false));
        }

        if (entity.getActivePotionEffect(Effects.ABSORPTION) == null) {
            entity.addPotionEffect(new EffectInstance(Effects.ABSORPTION, 200, amplifier, false, false));
        }
    }
}
