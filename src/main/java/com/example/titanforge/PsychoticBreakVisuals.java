package com.example.titanforge;

import net.minecraft.entity.MobEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.server.ServerWorld;

public final class PsychoticBreakVisuals {
    private PsychoticBreakVisuals() {}

    public static void start(MobEntity mob) {
        if (!(mob.world instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) mob.world;
        world.spawnParticle(ParticleTypes.WITCH,
                mob.getPosX(), mob.getPosY() + mob.getHeight() * 0.65D, mob.getPosZ(),
                36, 0.35D, 0.55D, 0.35D, 0.08D);
        world.spawnParticle(ParticleTypes.DAMAGE_INDICATOR,
                mob.getPosX(), mob.getPosY() + mob.getHeight() * 0.8D, mob.getPosZ(),
                14, 0.25D, 0.35D, 0.25D, 0.05D);
        world.playSound(null, mob.getPosition(), SoundEvents.ENTITY_ENDERMAN_SCREAM,
                SoundCategory.HOSTILE, 0.65F, 0.55F);
        mob.getPersistentData().putBoolean("TF_PsychoticActive", true);
    }

    public static void tick(MobEntity mob) {
        if (!(mob.world instanceof ServerWorld) || mob.ticksExisted % 4 != 0) return;
        ServerWorld world = (ServerWorld) mob.world;
        world.spawnParticle(ParticleTypes.WITCH,
                mob.getPosX() + (world.rand.nextDouble() - 0.5D) * mob.getWidth(),
                mob.getPosY() + world.rand.nextDouble() * mob.getHeight(),
                mob.getPosZ() + (world.rand.nextDouble() - 0.5D) * mob.getWidth(),
                2, 0.02D, 0.03D, 0.02D, 0.0D);
        world.spawnParticle(ParticleTypes.SMOKE,
                mob.getPosX(), mob.getPosY() + mob.getHeight() * 0.9D, mob.getPosZ(),
                1, 0.12D, 0.12D, 0.12D, 0.01D);
    }

    public static void stop(MobEntity mob) {
        mob.getPersistentData().remove("TF_PsychoticActive");
        if (!(mob.world instanceof ServerWorld)) return;
        ((ServerWorld) mob.world).spawnParticle(ParticleTypes.REVERSE_PORTAL,
                mob.getPosX(), mob.getPosY() + mob.getHeight() * 0.5D, mob.getPosZ(),
                18, 0.3D, 0.5D, 0.3D, 0.04D);
    }
}
