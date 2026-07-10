package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.DamageSource;

import java.lang.reflect.Method;

public final class CustomNpcChaosBridge {
    private static final String CUSTOM_NPC_PREFIX = "noppes.npcs";

    private CustomNpcChaosBridge() {}

    public static boolean isCustomNpc(LivingEntity entity) {
        return entity != null && entity.getClass().getName().startsWith(CUSTOM_NPC_PREFIX);
    }

    public static void forceTarget(MobEntity attacker, LivingEntity target) {
        attacker.setAttackTarget(target);
        attacker.setRevengeTarget(target);
        attacker.getNavigator().tryMoveToEntityLiving(target, 1.25D);

        invokeIfPresent(attacker, "setAttackTarget", LivingEntity.class, target);
        invokeIfPresent(attacker, "setRevengeTarget", LivingEntity.class, target);
    }

    public static void forceMeleePulse(MobEntity attacker, LivingEntity target) {
        if (!isCustomNpc(attacker) || target == null || !target.isAlive()) return;
        if (attacker.getDistanceSq(target) > 6.25D) return;
        if (attacker.ticksExisted % 20 != 0) return;

        target.hurtResistantTime = 0;
        target.attackEntityFrom(DamageSource.causeMobDamage(attacker), 3.0F);
        attacker.swingArm(net.minecraft.util.Hand.MAIN_HAND);
    }

    private static void invokeIfPresent(Object target, String name, Class<?> argType, Object arg) {
        try {
            Method method = target.getClass().getMethod(name, argType);
            method.setAccessible(true);
            method.invoke(target, arg);
        } catch (Throwable ignored) {}
    }
}
