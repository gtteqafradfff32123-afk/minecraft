package com.example.titanforge.entities.ai;

import com.example.titanforge.entities.ShadowEntity;
import com.example.titanforge.liminal.LiminalManager;
import com.example.titanforge.liminal.reward.DefeatedShadowTag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.EnumSet;
import java.util.UUID;

public class ShadowStalkGoal extends Goal {
    private final ShadowEntity shadow;
    private PlayerEntity target;

    private enum Mood { WATCHING, STALKING, LURKING, CLOSING }
    private Mood mood = Mood.WATCHING;
    private int moodTicks = 0;
    private int repositionCooldown = 0;
    private int wanderCooldown = 0;

    public ShadowStalkGoal(ShadowEntity shadow) {
        this.shadow = shadow;
        this.setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean shouldExecute() {
        if (DefeatedShadowTag.isDefeated(shadow)) return false;
        UUID owner = shadow.getOwnerId().orElse(null);
        if (owner == null) return false;
        this.target = shadow.world.getPlayerByUuid(owner);
        return target != null && target.isAlive();
    }

    @Override
    public boolean shouldContinueExecuting() {
        if (DefeatedShadowTag.isDefeated(shadow)) return false;
        return target != null && target.isAlive();
    }

    @Override
    public void tick() {
        if (target == null) return;
        moodTicks++;
        if (repositionCooldown > 0) repositionCooldown--;
        if (wanderCooldown > 0) wanderCooldown--;

        UUID owner = shadow.getOwnerId().orElse(null);
        LiminalManager.State st = owner == null ? null : LiminalManager.getState(owner);
        if (st != null && !shadow.isAggressive()) {
            if (st.shadowBehavior == LiminalManager.SHADOW_BEHAVIOR_STAY) {
                holdPosition(st);
                return;
            }
            if (st.shadowBehavior == LiminalManager.SHADOW_BEHAVIOR_WANDER) {
                wanderWithoutFollowing(st);
                return;
            }
        }

        shadow.getLookController().setLookPositionWithEntity(target, 30F, 30F);

        double dist = shadow.getDistance(target);
        boolean seen = isSeenByPlayer(target, shadow);

        if (seen) {
            handleWhileWatched(dist);
        } else {
            handleWhileUnseen(dist);
        }
    }

    private void handleWhileWatched(double dist) {
        shadow.getNavigator().clearPath();
        mood = Mood.WATCHING;

        if (dist < 8 && moodTicks % 60 == 0) {
            retreatToConcealment();
        }
    }

    private void handleWhileUnseen(double dist) {
        if (dist > 25) {
            mood = Mood.STALKING;
            if (repositionCooldown == 0) {
                blinkCloser();
                repositionCooldown = 40;
            }
        } else if (dist > 3) {
            mood = Mood.CLOSING;
            double speed = dist > 12 ? 1.4D : 1.0D;
            shadow.getNavigator().tryMoveToEntityLiving(target, speed);
        } else {
            mood = Mood.LURKING;
            if (!isSeenByPlayer(target, shadow)) {
                blinkCloser();
            }
        }
    }

    private void holdPosition(LiminalManager.State st) {
        shadow.getLookController().setLookPositionWithEntity(target, 20F, 20F);
        BlockPos hold = st.shadowHoldPos;
        if (hold == null) {
            st.shadowHoldPos = shadow.getPosition();
            shadow.getNavigator().clearPath();
            return;
        }

        if (shadow.getDistanceSq(hold.getX() + 0.5, hold.getY(), hold.getZ() + 0.5) > 2.25D) {
            shadow.getNavigator().tryMoveToXYZ(hold.getX() + 0.5, hold.getY(), hold.getZ() + 0.5, 1.0D);
        } else {
            shadow.getNavigator().clearPath();
        }
    }

    private void wanderWithoutFollowing(LiminalManager.State st) {
        if (!shadow.getNavigator().noPath() && wanderCooldown > 0) return;
        wanderCooldown = 80 + shadow.world.rand.nextInt(80);

        for (int i = 0; i < 12; i++) {
            double angle = shadow.world.rand.nextDouble() * Math.PI * 2;
            double dist = 25 + shadow.world.rand.nextDouble() * 55;
            double x = st.center.getX() + 0.5 + Math.cos(angle) * dist;
            double z = st.center.getZ() + 0.5 + Math.sin(angle) * dist;
            if (target.getDistanceSq(x, target.getPosY(), z) < 400D) continue;
            double y = findSafeY(x, target.getPosY(), z);
            if (y > 0) {
                shadow.getNavigator().tryMoveToXYZ(x, y, z, 0.9D);
                return;
            }
        }
        shadow.getNavigator().clearPath();
    }

    private void blinkCloser() {
        Vector3d look = target.getLookVec();
        double behindDist = 8 + shadow.world.rand.nextInt(6);
        double bx = target.getPosX() - look.x * behindDist;
        double bz = target.getPosZ() - look.z * behindDist;
        double by = findSafeY(bx, target.getPosY(), bz);
        if (by > 0) {
            shadow.setPositionAndUpdate(bx, by, bz);
            ((ServerWorld) shadow.world).spawnParticle(
                net.minecraft.particles.ParticleTypes.SMOKE, bx, by+1, bz, 15, 0.3,0.5,0.3, 0.01);
        }
    }

    private void retreatToConcealment() {
        Vector3d look = target.getLookVec();
        double bx = target.getPosX() - look.x * 20;
        double bz = target.getPosZ() - look.z * 20;
        shadow.getNavigator().tryMoveToXYZ(bx, target.getPosY(), bz, 1.2D);
    }

    private double findSafeY(double x, double startY, double z) {
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos p = new BlockPos(x, startY+dy, z);
            if (!shadow.world.getBlockState(p).isSolid()
                && shadow.world.getBlockState(p.down()).isSolid())
                return startY + dy;
        }
        return -1;
    }

    public static boolean isSeenByPlayer(PlayerEntity player, ShadowEntity shadow) {
        Vector3d look = player.getLook(1.0F).normalize();
        Vector3d toShadow = new Vector3d(
            shadow.getPosX() - player.getPosX(),
            shadow.getPosYEye() - player.getPosYEye(),
            shadow.getPosZ() - player.getPosZ());
        double len = toShadow.length();
        if (len < 0.01) return true;
        toShadow = toShadow.normalize();
        double dot = look.dotProduct(toShadow);
        if (dot > 0.6) {
            return player.canEntityBeSeen(shadow);
        }
        return false;
    }
}
