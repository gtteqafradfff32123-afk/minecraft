package com.example.titanforge.backrooms;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.server.ServerWorld;

public final class FootprintTrailManager {
    public static void tick(ServerWorld w, ServerPlayerEntity p, BackroomsSession s) {
        if (s.pressure < 35 || s.ticks % 16 != 0) return;
        double a = (s.seed + s.ticks) * .017;
        double x = p.getPosX() + Math.cos(a) * 5.0;
        double z = p.getPosZ() + Math.sin(a) * 5.0;
        w.spawnParticle(ParticleTypes.ASH, x, p.getPosY() + .05, z, 2, .12, .01, .12, 0);
    }
}
