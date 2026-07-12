package com.example.titanforge.liminal.screen;

import com.example.titanforge.liminal.LiminalManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.Random;

public final class LiminalWallParticles {
    private static final int WALL_INNER = 97;
    private static final int WALL_OUTER = 101;

    private static final IParticleData[] DARK = {
        ParticleTypes.SMOKE,
        ParticleTypes.LARGE_SMOKE,
        ParticleTypes.SOUL_FIRE_FLAME
    };

    public static void tick(ServerWorld world, ServerPlayerEntity player, LiminalManager.State state) {
        Random r = world.rand;
        BlockPos center = state.center;
        int count = 8 + r.nextInt(9);

        for (int i = 0; i < count; i++) {
            double angle = r.nextDouble() * Math.PI * 2.0D;
            double radius = WALL_INNER + r.nextDouble() * 1.5D;
            double wx = center.getX() + 0.5D + Math.cos(angle) * radius;
            double wz = center.getZ() + 0.5D + Math.sin(angle) * radius;
            double wy = 5 + r.nextDouble() * 195;
            IParticleData p = DARK[r.nextInt(DARK.length)];
            world.spawnParticle(p, wx, wy, wz, 1, 0.1D, 0.3D, 0.1D, 0.003D);
        }

        if (r.nextInt(8) == 0) {
            double angle = r.nextDouble() * Math.PI * 2.0D;
            double radius = WALL_INNER + 0.5D + r.nextDouble();
            double gx = center.getX() + 0.5D + Math.cos(angle) * radius;
            double gz = center.getZ() + 0.5D + Math.sin(angle) * radius;
            double gy = 10 + r.nextDouble() * 180;
            world.spawnParticle(ParticleTypes.DRAGON_BREATH, gx, gy, gz, 6, 0.3D, 0.3D, 0.3D, 0.01D);
        }

        if (r.nextInt(3) == 0 && player != null) {
            double px = player.getPosX() + (r.nextDouble() - 0.5D) * 60;
            double py = player.getPosY() + r.nextDouble() * 20;
            double pz = player.getPosZ() + (r.nextDouble() - 0.5D) * 60;
            world.spawnParticle(ParticleTypes.ASH, px, py, pz, 1, 0, 0, 0, 0.01D);
        }
    }

    private LiminalWallParticles() {}
}
