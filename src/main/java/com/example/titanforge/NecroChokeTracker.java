package com.example.titanforge;

import com.example.titanforge.entities.BlindGolemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.server.ServerWorld;

import java.util.*;

public class NecroChokeTracker {
    private static final Map<UUID, ChokeInstance> choked = new HashMap<>();
    private static final Map<UUID, Integer> activeGolems = new HashMap<>();

    private static class ChokeInstance {
        final LivingEntity target;
        final ServerPlayerEntity owner;
        final int initialDuration;
        int duration;

        ChokeInstance(LivingEntity target, ServerPlayerEntity owner, int duration) {
            this.target = target;
            this.owner = owner;
            this.initialDuration = duration;
            this.duration = duration;
        }
    }

    public static void mark(LivingEntity target, ServerPlayerEntity owner, int duration) {
        choked.put(target.getUniqueID(), new ChokeInstance(target, owner, duration));
    }

    public static void tick(ServerWorld world) {
        Iterator<Map.Entry<UUID, ChokeInstance>> it = choked.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ChokeInstance> entry = it.next();
            ChokeInstance inst = entry.getValue();
            if (inst.target == null || !inst.target.isAlive() || --inst.duration <= 0) {
                it.remove();
                continue;
            }
            if ((inst.initialDuration - inst.duration) % 20 == 0) {
                inst.target.attackEntityFrom(ModDamageSources.NECROTIC_UNDERTOW, 3.0F);
            }
            // Hands from the ground visual
            if (inst.duration % 4 == 0) {
                world.spawnParticle(ParticleTypes.SOUL,
                    inst.target.getPosX() + (world.rand.nextDouble() - 0.5) * 1.5,
                    inst.target.getPosY(),
                    inst.target.getPosZ() + (world.rand.nextDouble() - 0.5) * 1.5,
                    1, 0.0, 1.0, 0.0, 0.02);
                world.spawnParticle(ParticleTypes.SMOKE,
                    inst.target.getPosX() + (world.rand.nextDouble() - 0.5) * 1.5,
                    inst.target.getPosY() + 0.2,
                    inst.target.getPosZ() + (world.rand.nextDouble() - 0.5) * 1.5,
                    2, 0.0, 0.3, 0.0, 0.01);
            }
            if (inst.target.ticksExisted % 40 == 0) {
                world.playSound(null, inst.target.getPosition(),
                    SoundEvents.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR,
                    SoundCategory.HOSTILE, 0.3F, 0.3F);
            }
        }
    }

    public static void onDeath(LivingEntity dead, ServerWorld world) {
        if (dead instanceof BlindGolemEntity) {
            onGolemRemoved(((BlindGolemEntity) dead).getOwnerId());
            return;
        }

        UUID id = dead.getUniqueID();
        ChokeInstance inst = choked.get(id);
        if (inst == null) {
            com.example.titanforge.TitanForge.LOGGER.info("[NecroUndertow] entity {} died but not in choked map", dead.getName().getString());
            return;
        }

        UUID ownerId = inst.owner.getUniqueID();
        if (activeGolems.getOrDefault(ownerId, 0) >= 1) {
            com.example.titanforge.TitanForge.LOGGER.info("[NecroUndertow] owner already has a golem");
            return;
        }

        BlindGolemEntity golem = ModEntities.BLIND_GOLEM.get().create(world);
        if (golem != null) {
            golem.setPosition(dead.getPosX(), dead.getPosY(), dead.getPosZ());
            golem.setOwner(ownerId);
            world.addEntity(golem);
            activeGolems.put(ownerId, activeGolems.getOrDefault(ownerId, 0) + 1);
            // Spawn effects
            world.spawnParticle(net.minecraft.particles.ParticleTypes.SOUL,
                dead.getPosX(), dead.getPosY() + 1, dead.getPosZ(), 50, 0.8, 1.0, 0.8, 0.1);
            world.spawnParticle(net.minecraft.particles.ParticleTypes.SMOKE,
                dead.getPosX(), dead.getPosY() + 0.5, dead.getPosZ(), 30, 0.5, 0.5, 0.5, 0.02);
            world.playSound(null, new net.minecraft.util.math.BlockPos(dead.getPosX(), dead.getPosY(), dead.getPosZ()),
                net.minecraft.util.SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED,
                net.minecraft.util.SoundCategory.HOSTILE, 0.8F, 0.3F);
            com.example.titanforge.TitanForge.LOGGER.info("[NecroUndertow] golem spawned for {} at death of {}",
                inst.owner.getName().getString(), dead.getName().getString());
        } else {
            com.example.titanforge.TitanForge.LOGGER.warn("[NecroUndertow] golem entity creation returned null");
        }
    }

    public static void onGolemRemoved(UUID ownerId) {
        if (ownerId == null) return;
        int count = activeGolems.getOrDefault(ownerId, 0) - 1;
        if (count <= 0) activeGolems.remove(ownerId);
        else activeGolems.put(ownerId, count);
    }

    public static void cleanupOwner(UUID ownerId) {
        choked.entrySet().removeIf(e -> e.getValue().owner.getUniqueID().equals(ownerId));
        activeGolems.remove(ownerId);
    }
}
