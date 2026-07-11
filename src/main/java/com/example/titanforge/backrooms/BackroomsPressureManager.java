package com.example.titanforge.backrooms;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.server.ServerWorld;

public final class BackroomsPressureManager {
    private static final int MAX_PRESSURE = 100;

    private BackroomsPressureManager() {}

    public static void tick(ServerWorld world, ServerPlayerEntity player, BackroomsSession session) {
        if (session.finished) return;

        int oldPressure = session.pressure;

        if (session.ticks % 40 == 0) {
            session.pressure = Math.min(MAX_PRESSURE, session.pressure + 1);
        }

        if (session.pressure >= 25 && session.pressure < 60) {
            if (session.ticks % 100 == 0 && world.rand.nextInt(3) == 0) {
                player.addPotionEffect(new EffectInstance(
                        Effects.NAUSEA, 100, 0, false, false));
            }
        }

        if (session.pressure >= 60) {
            if (session.ticks % 60 == 0) {
                player.addPotionEffect(new EffectInstance(
                        Effects.NAUSEA, 120, 1, false, false));
                player.addPotionEffect(new EffectInstance(
                        Effects.WEAKNESS, 120, 0, false, false));
            }
        }

        if (session.pressure >= 85 && session.ticks % 80 == 0) {
            player.addPotionEffect(new EffectInstance(
                    Effects.BLINDNESS, 60, 0, false, false));
        }
    }

    public static void add(BackroomsSession session, int amount) {
        session.pressure = Math.min(MAX_PRESSURE, session.pressure + amount);
    }

    public static void foundError(BackroomsSession session) {
        session.errorsFound++;
        session.pressure = Math.max(0, session.pressure - 15);
    }
}
