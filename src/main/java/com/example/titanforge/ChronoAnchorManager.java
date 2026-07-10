package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.server.ServerWorld;

public final class ChronoAnchorManager {
    private static final String ACTIVE = "TF_ChronoActive";
    private static final String END = "TF_ChronoEnd";
    private static final String DAMAGE = "TF_ChronoDamage";
    private static final String OLD_NO_AI = "TF_ChronoOldNoAI";
    private static final String OLD_NO_GRAVITY = "TF_ChronoOldNoGravity";

    private ChronoAnchorManager() {}

    public static void freeze(LivingEntity target, int durationTicks) {
        if (target.world.isRemote) return;
        CompoundNBT data = target.getPersistentData();

        if (!data.getBoolean(ACTIVE)) {
            data.putBoolean(OLD_NO_GRAVITY, target.hasNoGravity());
            if (target instanceof MobEntity) {
                data.putBoolean(OLD_NO_AI, ((MobEntity) target).isAIDisabled());
            }
            data.putFloat(DAMAGE, 0.0F);
        }

        data.putBoolean(ACTIVE, true);
        data.putLong(END, target.world.getGameTime() + durationTicks);
        if (target instanceof MobEntity) ((MobEntity) target).setNoAI(true);
        target.setNoGravity(true);
        target.setMotion(0.0D, 0.0D, 0.0D);
        target.velocityChanged = true;

        ServerWorld world = (ServerWorld) target.world;
        world.spawnParticle(ParticleTypes.END_ROD, target.getPosX(), target.getPosY() + target.getHeight() * 0.5D,
                target.getPosZ(), 20, 0.4D, 0.8D, 0.4D, 0.05D);
    }

    public static boolean captureDamage(LivingEntity target, float amount) {
        if (target.world.isRemote) return false;
        CompoundNBT data = target.getPersistentData();
        if (!data.getBoolean(ACTIVE)) return false;
        data.putFloat(DAMAGE, data.getFloat(DAMAGE) + Math.max(0.0F, amount));
        return true;
    }

    public static void tick(LivingEntity target) {
        if (target.world.isRemote) return;
        CompoundNBT data = target.getPersistentData();
        if (!data.getBoolean(ACTIVE)) return;

        target.setMotion(0.0D, 0.0D, 0.0D);
        target.velocityChanged = true;
        if (target.world.getGameTime() < data.getLong(END)) return;

        float accumulated = data.getFloat(DAMAGE);
        boolean oldNoGravity = data.getBoolean(OLD_NO_GRAVITY);
        boolean oldNoAi = data.getBoolean(OLD_NO_AI);

        clearTags(data);
        target.setNoGravity(oldNoGravity);
        if (target instanceof MobEntity) ((MobEntity) target).setNoAI(oldNoAi);

        if (accumulated > 0.0F && target.isAlive()) {
            target.hurtResistantTime = 0;
            target.attackEntityFrom(ModDamageSources.CHRONO_CRUSH, accumulated);
            ((ServerWorld) target.world).spawnParticle(ParticleTypes.END_ROD,
                    target.getPosX(), target.getPosY() + target.getHeight() * 0.5D, target.getPosZ(),
                    36, 0.55D, 0.9D, 0.55D, 0.15D);
        }
    }

    public static void clear(LivingEntity target) {
        CompoundNBT data = target.getPersistentData();
        if (!data.getBoolean(ACTIVE)) return;
        target.setNoGravity(data.getBoolean(OLD_NO_GRAVITY));
        if (target instanceof MobEntity) ((MobEntity) target).setNoAI(data.getBoolean(OLD_NO_AI));
        clearTags(data);
    }

    private static void clearTags(CompoundNBT data) {
        data.remove(ACTIVE);
        data.remove(END);
        data.remove(DAMAGE);
        data.remove(OLD_NO_AI);
        data.remove(OLD_NO_GRAVITY);
    }
}
