package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ChaosAiManager {
    private static final Map<UUID, ChaosState> ACTIVE = new HashMap<>();

    private static final class ChaosState {
        final MobEntity mob;
        final UUID caster;
        final Goal targetGoal;
        final LivingEntity oldTarget;
        long endTick;

        ChaosState(MobEntity mob, UUID caster, Goal targetGoal, LivingEntity oldTarget, long endTick) {
            this.mob = mob;
            this.caster = caster;
            this.targetGoal = targetGoal;
            this.oldTarget = oldTarget;
            this.endTick = endTick;
        }
    }

    private ChaosAiManager() {}

    public static void apply(MobEntity mob, PlayerEntity caster, int duration) {
        ChaosState old = ACTIVE.remove(mob.getUniqueID());
        if (old != null) old.mob.targetSelector.removeGoal(old.targetGoal);

        PsychoticBreakVisuals.start(mob);

        Goal chaosGoal = new NearestAttackableTargetGoal<>(mob, LivingEntity.class, 1, true, false,
                candidate -> isValidTarget(candidate, mob, caster.getUniqueID()));
        mob.targetSelector.addGoal(0, chaosGoal);
        ACTIVE.put(mob.getUniqueID(), new ChaosState(
                mob, caster.getUniqueID(), chaosGoal, mob.getAttackTarget(), mob.world.getGameTime() + duration));
        mob.setAttackTarget(findNearest(mob, caster.getUniqueID()));
    }

    public static void tick() {
        Iterator<Map.Entry<UUID, ChaosState>> it = ACTIVE.entrySet().iterator();
        while (it.hasNext()) {
            ChaosState state = it.next().getValue();
            MobEntity mob = state.mob;
            if (mob == null || !mob.isAlive()) {
                it.remove();
                continue;
            }
            if (mob.world.getGameTime() >= state.endTick) {
                PsychoticBreakVisuals.stop(mob);
                mob.targetSelector.removeGoal(state.targetGoal);
                if (state.oldTarget != null && state.oldTarget.isAlive()) mob.setAttackTarget(state.oldTarget);
                else mob.setAttackTarget(null);
                it.remove();
                continue;
            }

            PsychoticBreakVisuals.tick(mob);

            LivingEntity forced = findNearest(mob, state.caster);
            if (forced != null) {
                CustomNpcChaosBridge.forceTarget(mob, forced);
                CustomNpcChaosBridge.forceMeleePulse(mob, forced);
            }

            LivingEntity current = mob.getAttackTarget();
            if (!isValidTarget(current, mob, state.caster) || mob.ticksExisted % 20 == 0) {
                mob.setAttackTarget(findNearest(mob, state.caster));
            }
        }
    }

    public static void clear(MobEntity mob) {
        ChaosState state = ACTIVE.remove(mob.getUniqueID());
        if (state != null) mob.targetSelector.removeGoal(state.targetGoal);
    }

    private static LivingEntity findNearest(MobEntity mob, UUID caster) {
        AxisAlignedBB box = mob.getBoundingBox().grow(20.0D);
        List<LivingEntity> entities = mob.world.getEntitiesWithinAABB(LivingEntity.class, box,
                e -> isValidTarget(e, mob, caster));
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : entities) {
            double distance = mob.getDistanceSq(entity);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = entity;
            }
        }
        return best;
    }

    private static boolean isValidTarget(LivingEntity candidate, MobEntity mob, UUID caster) {
        if (candidate == null || !candidate.isAlive() || candidate == mob) return false;
        if (candidate.getUniqueID().equals(caster)) return false;
        if (candidate instanceof PlayerEntity) return false;
        if (ChaosDevourHandler.isThrall(candidate) || SoulReaperHandler.isSoulCopy(candidate)) return false;
        return true;
    }
}
