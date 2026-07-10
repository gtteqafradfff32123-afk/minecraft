package com.example.titanforge.backrooms;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.server.ServerWorld;

public final class BackroomsPressureManager {
    private BackroomsPressureManager() {}

    public static void tick(ServerWorld world, ServerPlayerEntity player, BackroomsSession session) {
        if (session.pressure <= 0) return;
        if (session.ticks % 100 != 0) return;
        session.pressure = Math.max(0, session.pressure - 1);
        if (session.pressure > 80) {
            player.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 40, 1, false, false));
        }
    }

    public static void add(BackroomsSession session, int amount) {
        session.pressure = Math.min(100, session.pressure + amount);
    }

    public static void foundError(BackroomsSession session) {
        session.errorsFound++;
    }
}
