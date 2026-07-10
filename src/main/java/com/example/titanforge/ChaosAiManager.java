package com.example.titanforge;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.WaterAvoidingRandomWalkingGoal;

import java.util.*;

public class ChaosAiManager {
    private static final Map<UUID, MobEntity> crazed = new HashMap<>();

    public static void apply(MobEntity mob, int duration) {
        crazed.put(mob.getUniqueID(), mob);
        mob.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(mob, LivingEntity.class,
                10, true, false,
                e -> e != mob && e.isAlive()));
        if (mob instanceof CreatureEntity)
            mob.goalSelector.addGoal(1, new WaterAvoidingRandomWalkingGoal((CreatureEntity) mob, 1.0D));
    }

    public static void tick() {
        crazed.entrySet().removeIf(e -> !e.getValue().isAlive());
    }
}
