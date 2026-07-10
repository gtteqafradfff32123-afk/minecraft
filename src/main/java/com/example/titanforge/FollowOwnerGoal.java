package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.List;

public class FollowOwnerGoal extends Goal {

    private final MobEntity thrall;
    private final PlayerEntity owner;
    private final double teleportDistance;
    private final double minDistance;
    private final double speed;
    private final PathNavigator navigator;
    private int pathfindCooldown;
    private int attackCooldown;
    private int scanCooldown;

    public FollowOwnerGoal(MobEntity thrall, PlayerEntity owner, double teleportDist, double minDist, double speed) {
        this.thrall = thrall;
        this.owner = owner;
        this.teleportDistance = teleportDist;
        this.minDistance = minDist;
        this.speed = speed;
        this.navigator = thrall.getNavigator();
        this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    @Override
    public boolean shouldExecute() {
        if (owner == null || !owner.isAlive()) return false;
        if (!thrall.isAlive()) return false;
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (owner == null || !owner.isAlive()) return false;
        if (!thrall.isAlive()) return false;
        return true;
    }

    @Override
    public void startExecuting() {
        pathfindCooldown = 0;
        attackCooldown = 0;
        scanCooldown = 0;
    }

    @Override
    public void resetTask() {
        navigator.clearPath();
    }

    @Override
    public void tick() {
        LivingEntity target = thrall.getAttackTarget();

        if (target != null && target.isAlive()) {
            if (target == owner || target instanceof PlayerEntity || ChaosDevourHandler.isThrall(target)) {
                thrall.setAttackTarget(null);
                target = null;
            }
        }

        if (target == null) {
            if (--scanCooldown <= 0) {
                scanCooldown = 20;
                target = acquireTarget();
                if (target != null) {
                    thrall.setAttackTarget(target);
                }
            }
        }

        if (target != null && target.isAlive()) {
            double distSq = thrall.getDistanceSq(target);
            if (distSq < 6.0D) {
                thrall.getLookController().setLookPositionWithEntity(target, 30.0f, 30.0f);
                if (attackCooldown <= 0) {
                    attackCooldown = 20;
                    target.attackEntityFrom(DamageSource.causeMobDamage(thrall), 3.0F);
                }
            } else {
                navigator.tryMoveToEntityLiving(target, speed * 1.2);
            }
        } else {
            thrall.setAttackTarget(null);
            if (--pathfindCooldown <= 0) {
                pathfindCooldown = 10;
                double distSq = thrall.getDistanceSq(owner);

                if (distSq > (teleportDistance * teleportDistance)) {
                    BlockPos ownerPos = owner.getPosition();
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            BlockPos tryPos = ownerPos.add(dx, 0, dz);
                            if (thrall.world.isAirBlock(tryPos) && thrall.world.isAirBlock(tryPos.up()) && !thrall.world.isAirBlock(tryPos.down())) {
                                thrall.setPositionAndRotation(tryPos.getX() + 0.5, tryPos.getY(), tryPos.getZ() + 0.5,
                                    thrall.rotationYaw, thrall.rotationPitch);
                                navigator.clearPath();
                                return;
                            }
                        }
                    }
                    thrall.setPositionAndRotation(ownerPos.getX() + 0.5, ownerPos.getY(), ownerPos.getZ() + 0.5,
                        thrall.rotationYaw, thrall.rotationPitch);
                    navigator.clearPath();
                } else {
                    navigator.tryMoveToEntityLiving(owner, speed);
                }
            }
        }

        if (attackCooldown > 0) attackCooldown--;
    }

    private LivingEntity acquireTarget() {
        AxisAlignedBB searchArea = thrall.getBoundingBox().grow(15.0);
        List<LivingEntity> entities = thrall.world.getEntitiesWithinAABB(LivingEntity.class, searchArea);
        for (LivingEntity e : entities) {
            if (e == thrall) continue;
            if (ChaosDevourHandler.isThrall(e)) continue;
            if (e == owner) continue;
            if (e instanceof PlayerEntity) continue;
            if (!e.isAlive()) continue;
            if (e.getAttackingEntity() == owner || e.getRevengeTarget() == owner) return e;
        }
        return null;
    }
}
