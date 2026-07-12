package com.example.titanforge.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.EnumSet;
import java.util.UUID;

public final class GraveglassColossusEntity extends MonsterEntity {
    private UUID ownerId;
    private int lifespan = 30 * 20;

    public GraveglassColossusEntity(EntityType<? extends GraveglassColossusEntity> type, World world) {
        super(type, world);
        this.experienceValue = 28;
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MonsterEntity.func_234295_eP_()
            .createMutableAttribute(Attributes.MAX_HEALTH, 72.0D)
            .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.225D)
            .createMutableAttribute(Attributes.ATTACK_DAMAGE, 11.0D)
            .createMutableAttribute(Attributes.ARMOR, 9.0D)
            .createMutableAttribute(Attributes.ARMOR_TOUGHNESS, 3.0D)
            .createMutableAttribute(Attributes.KNOCKBACK_RESISTANCE, 0.85D)
            .createMutableAttribute(Attributes.FOLLOW_RANGE, 36.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, false));
        this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.0D, 6.0F, 24.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 0.75D));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2,
            new NearestAttackableTargetGoal<>(this, MonsterEntity.class,
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
            if (ticksExisted % 5 == 0) {
                double x = getPosX() + (rand.nextDouble() - 0.5D) * 0.9D;
                double y = getPosY() + 0.7D + rand.nextDouble() * 1.7D;
                double z = getPosZ() + (rand.nextDouble() - 0.5D) * 0.9D;
                world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0D, 0.012D, 0.0D);
            }
            return;
        }

        if (--lifespan <= 0) {
            ServerWorld sw = (ServerWorld) world;
            sw.spawnParticle(ParticleTypes.SOUL,
                getPosX(), getPosY() + 1, getPosZ(), 40, 0.5, 0.8, 0.5, 0.05);
            sw.spawnParticle(ParticleTypes.LARGE_SMOKE,
                getPosX(), getPosY() + 1, getPosZ(), 20, 0.3, 0.5, 0.3, 0.03);
            com.example.titanforge.NecroChokeTracker.onGolemRemoved(ownerId);
            this.remove();
        }
    }

    @Override
    public boolean attackEntityAsMob(net.minecraft.entity.Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            living.setMotion(living.getMotion().add(0.0D, 0.34D, 0.0D));
            living.velocityChanged = true;
        }
        return hit;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source.isExplosion()) amount *= 0.55F;
        return super.attackEntityFrom(source, amount);
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    static class FollowOwnerGoal extends Goal {
        private final GraveglassColossusEntity colossus;
        private final double speed;
        private final float minDist;
        private final float maxDist;

        FollowOwnerGoal(GraveglassColossusEntity colossus, double speed, float minDist, float maxDist) {
            this.colossus = colossus;
            this.speed = speed;
            this.minDist = minDist * minDist;
            this.maxDist = maxDist * maxDist;
            this.setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean shouldExecute() {
            if (colossus.ownerId == null) return false;
            if (colossus.getAttackTarget() != null) return false;
            PlayerEntity owner = colossus.world.getPlayerByUuid(colossus.ownerId);
            if (owner == null || !owner.isAlive()) return false;
            return colossus.getDistanceSq(owner) > maxDist;
        }

        @Override
        public void tick() {
            if (colossus.ownerId == null) return;
            PlayerEntity owner = colossus.world.getPlayerByUuid(colossus.ownerId);
            if (owner == null) return;
            double dist = colossus.getDistanceSq(owner);
            if (dist > maxDist) {
                colossus.getNavigator().tryMoveToEntityLiving(owner, speed);
            } else if (dist < minDist) {
                colossus.getNavigator().clearPath();
            }
        }
    }
}
