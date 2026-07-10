package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Effects;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class PlagueEffect extends Effect {
    public PlagueEffect() {
        super(EffectType.HARMFUL, 0x4A5D23);
    }

    @Override
    public List<ItemStack> getCurativeItems() {
        return new ArrayList<>();
    }

    @Override
    public boolean isReady(int duration, int amplifier) {
        return true;
    }

    @Override
    public void performEffect(LivingEntity entity, int amplifier) {
        EffectInstance instance = entity.getActivePotionEffect(this);
        if (instance == null) return;

        int duration = instance.getDuration();
        World world = entity.world;

        if (!world.isRemote) {
            ((net.minecraft.world.server.ServerWorld)world).spawnParticle(ParticleTypes.SNEEZE,
                entity.getPosX() + (world.rand.nextDouble() - 0.5) * 0.5,
                entity.getPosY() + 1.0,
                entity.getPosZ() + (world.rand.nextDouble() - 0.5) * 0.5,
                1, 0, 0.05, 0, 0);
        }

        if (duration > 3600) {
            if (!world.isRemote && world.rand.nextFloat() < 0.01F) {
                entity.attackEntityFrom(ModDamageSources.PLAGUE, 1.0F + amplifier * 0.5F);
            }
        } else if (duration > 2400) {
            if (duration % 300 == 0) {
                applyRandomDebuffs(entity, false);
            }
            if (!world.isRemote && world.rand.nextFloat() < 0.05F) {
                entity.attackEntityFrom(ModDamageSources.PLAGUE, 2.0F + amplifier + world.rand.nextInt(3));
            }
        } else if (duration > 1200) {
            if (duration % 200 == 0) {
                applyRandomDebuffs(entity, true);
            }
            if (!world.isRemote && world.rand.nextFloat() < 0.10F) {
                entity.attackEntityFrom(ModDamageSources.PLAGUE, 6.0F + amplifier * 2.0F + world.rand.nextInt(5));
            }
        } else {
            if (duration % 60 == 0) {
                entity.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 100, 0, false, false));
                entity.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 100, 1, false, false));
                entity.addPotionEffect(new EffectInstance(Effects.POISON, 100, amplifier > 0 ? 1 : 0, false, false));
                entity.addPotionEffect(new EffectInstance(Effects.WITHER, 60, amplifier > 1 ? 1 : 0, false, false));
            }
            if (!world.isRemote && duration % 20 == 0) {
                float dmg = (1200 - duration) / 20.0F + 1.0F + amplifier;
                entity.attackEntityFrom(ModDamageSources.PLAGUE, dmg);
                entity.playSound(SoundEvents.ENTITY_GENERIC_HURT, 1.0F, 0.5F);
            }
            if (!world.isRemote) {
                ((net.minecraft.world.server.ServerWorld)world).spawnParticle(ParticleTypes.ASH,
                    entity.getPosX() + (world.rand.nextDouble() - 0.5),
                    entity.getPosY() + 1.0,
                    entity.getPosZ() + (world.rand.nextDouble() - 0.5),
                    1, 0, 0.1, 0, 0);
            }
            if (!world.isRemote && duration <= 1) {
                entity.attackEntityFrom(ModDamageSources.PLAGUE, 1000.0F);
            }
        }
    }

    private void applyRandomDebuffs(LivingEntity entity, boolean severe) {
        if (severe || entity.world.rand.nextBoolean()) {
            entity.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 200, 0, false, false));
            entity.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 200, 0, false, false));
            entity.addPotionEffect(new EffectInstance(Effects.POISON, 120, severe ? 1 : 0, false, false));
            if (severe) {
                entity.addPotionEffect(new EffectInstance(Effects.WITHER, 80, 0, false, false));
            }
        } else {
            entity.addPotionEffect(new EffectInstance(Effects.POISON, 80, 0, false, false));
        }
    }
}
