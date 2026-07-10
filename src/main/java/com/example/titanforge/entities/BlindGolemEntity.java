package com.example.titanforge.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.EnumSet;
import java.util.UUID;

public class BlindGolemEntity extends MonsterEntity {
    private UUID ownerId;
    private int lifespan = 30 * 20;

    public BlindGolemEntity(EntityType<? extends MonsterEntity> type, World world) {
        super(type, world);
    }

    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return MonsterEntity.func_233666_p_()
                .createMutableAttribute(Attributes.MAX_HEALTH, 40.0D)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.3D)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 7.0D)
                .createMutableAttribute(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(2, new FollowOwnerGoal(this, 1.0D, 6.0F, 24.0F));
        this.goalSelector.addGoal(3, new WaterAvoidingRandomWalkingGoal(this, 0.8D));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, MonsterEntity.class,
                10, true, false,
                e -> e != null && e.isAlive() && (ownerId == null || !e.getUniqueID().equals(ownerId))));
    }

    public void setOwner(UUID id) {
        this.ownerId = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public void livingTick() {
        super.livingTick();
        if (world.isRemote) {
            // Client-side visual effects
            if (rand.nextInt(8) == 0) {
                world.addParticle(net.minecraft.particles.ParticleTypes.SOUL,
                    getPosX() + (rand.nextDouble() - 0.5) * 1.2,
                    getPosY() + 0.2,
                    getPosZ() + (rand.nextDouble() - 0.5) * 1.2,
                    0, 0.05, 0);
            }
            if (rand.nextInt(12) == 0) {
                world.addParticle(net.minecraft.particles.ParticleTypes.ASH,
                    getPosX() + (rand.nextDouble() - 0.5) * 0.8,
                    getPosY() + 1.2 + rand.nextDouble() * 0.5,
                    getPosZ() + (rand.nextDouble() - 0.5) * 0.8,
                    0, 0.01, 0);
            }
            return;
        }

        if (--lifespan <= 0) {
            ServerWorld sw = (ServerWorld) world;
            sw.spawnParticle(net.minecraft.particles.ParticleTypes.SOUL,
                getPosX(), getPosY() + 1, getPosZ(), 40, 0.5, 0.8, 0.5, 0.05);
            sw.spawnParticle(net.minecraft.particles.ParticleTypes.LARGE_SMOKE,
                getPosX(), getPosY() + 1, getPosZ(), 20, 0.3, 0.5, 0.3, 0.03);
            com.example.titanforge.NecroChokeTracker.onGolemRemoved(ownerId);
            this.remove();
        }
    }

    static class FollowOwnerGoal extends Goal {
        private final BlindGolemEntity golem;
        private final double speed;
        private final float minDist;
        private final float maxDist;

        FollowOwnerGoal(BlindGolemEntity golem, double speed, float minDist, float maxDist) {
            this.golem = golem;
            this.speed = speed;
            this.minDist = minDist * minDist;
            this.maxDist = maxDist * maxDist;
            this.setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean shouldExecute() {
            if (golem.ownerId == null) return false;
            if (golem.getAttackTarget() != null) return false;
            PlayerEntity owner = golem.world.getPlayerByUuid(golem.ownerId);
            if (owner == null || !owner.isAlive()) return false;
            return golem.getDistanceSq(owner) > maxDist;
        }

        @Override
        public void tick() {
            if (golem.ownerId == null) return;
            PlayerEntity owner = golem.world.getPlayerByUuid(golem.ownerId);
            if (owner == null) return;
            double dist = golem.getDistanceSq(owner);
            if (dist > maxDist) {
                golem.getNavigator().tryMoveToEntityLiving(owner, speed);
            } else if (dist < minDist) {
                golem.getNavigator().clearPath();
            }
        }
    }
}
