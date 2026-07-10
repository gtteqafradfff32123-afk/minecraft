package com.example.titanforge.entities.ai;

import com.example.titanforge.entities.PlayerCopyEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.EnumSet;
import java.util.UUID;

public class PlayerCopyStalkGoal extends Goal {
    private final PlayerCopyEntity copy;
    private PlayerEntity target;

    private enum Mood { WATCHING, STALKING, LURKING, CLOSING }
    private Mood mood = Mood.WATCHING;
    private int moodTicks = 0;
    private int repositionCooldown = 0;

    public PlayerCopyStalkGoal(PlayerCopyEntity copy) {
        this.copy = copy;
        this.setMutexFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean shouldExecute() {
        UUID owner = copy.getOwnerId().orElse(null);
        if (owner == null) return false;
        this.target = copy.world.getPlayerByUuid(owner);
        return target != null && target.isAlive();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return target != null && target.isAlive();
    }

    @Override
    public void tick() {
        if (target == null) return;
        moodTicks++;
        if (repositionCooldown > 0) repositionCooldown--;

        copy.getLookController().setLookPositionWithEntity(target, 30F, 30F);

        double dist = copy.getDistance(target);
        boolean seen = isSeenByPlayer(target, copy);

        if (seen) {
            handleWhileWatched(dist);
        } else {
            handleWhileUnseen(dist);
        }
    }

    private void handleWhileWatched(double dist) {
        copy.getNavigator().clearPath();
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
            copy.getNavigator().tryMoveToEntityLiving(target, speed);
        } else {
            mood = Mood.LURKING;
            strikeFromBehind();
        }
    }

    private void blinkCloser() {
        Vector3d look = target.getLookVec();
        double behindDist = 8 + copy.world.rand.nextInt(6);
        double bx = target.getPosX() - look.x * behindDist;
        double bz = target.getPosZ() - look.z * behindDist;
        double by = findSafeY(bx, target.getPosY(), bz);
        if (by > 0) {
            copy.setPositionAndUpdate(bx, by, bz);
            ((ServerWorld) copy.world).spawnParticle(
                net.minecraft.particles.ParticleTypes.SMOKE, bx, by+1, bz, 15, 0.3,0.5,0.3, 0.01);
        }
    }

    private void retreatToConcealment() {
        Vector3d look = target.getLookVec();
        double bx = target.getPosX() - look.x * 20;
        double bz = target.getPosZ() - look.z * 20;
        copy.getNavigator().tryMoveToXYZ(bx, target.getPosY(), bz, 1.2D);
    }

    private void strikeFromBehind() {
        if (!isSeenByPlayer(target, copy)) {
            target.addPotionEffect(new EffectInstance(Effects.WITHER, 60, 1));
            target.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 100, 0));
            target.world.playSound(null, target.getPosition(),
                net.minecraft.util.SoundEvents.ENTITY_WITHER_AMBIENT,
                net.minecraft.util.SoundCategory.HOSTILE, 0.6F, 0.3F);
            blinkCloser();
        }
    }

    private double findSafeY(double x, double startY, double z) {
        for (int dy = 2; dy >= -4; dy--) {
            BlockPos p = new BlockPos(x, startY+dy, z);
            if (!copy.world.getBlockState(p).isSolid()
                && copy.world.getBlockState(p.down()).isSolid())
                return startY + dy;
        }
        return -1;
    }

    public static boolean isSeenByPlayer(PlayerEntity player, PlayerCopyEntity copy) {
        Vector3d look = player.getLook(1.0F).normalize();
        Vector3d toCopy = new Vector3d(
            copy.getPosX() - player.getPosX(),
            copy.getPosYEye() - player.getPosYEye(),
            copy.getPosZ() - player.getPosZ());
        double len = toCopy.length();
        if (len < 0.01) return true;
        toCopy = toCopy.normalize();
        double dot = look.dotProduct(toCopy);
        if (dot > 0.6) {
            return player.canEntityBeSeen(copy);
        }
        return false;
    }
}
