package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class VoidVortexManager {
    private static final List<Vortex> ACTIVE = new ArrayList<>();

    private static final class Vortex {
        final ServerWorld world;
        final Vector3d center;
        final UUID owner;
        int life = 60;

        Vortex(ServerWorld world, Vector3d center, UUID owner) {
            this.world = world;
            this.center = center;
            this.owner = owner;
        }
    }

    private VoidVortexManager() {}

    public static void spawn(ServerWorld world, Vector3d center, PlayerEntity owner) {
        ACTIVE.add(new Vortex(world, center, owner.getUniqueID()));
    }

    public static void tick(ServerWorld world) {
        Iterator<Vortex> iterator = ACTIVE.iterator();
        while (iterator.hasNext()) {
            Vortex vortex = iterator.next();
            if (vortex.world != world) continue;
            if (--vortex.life <= 0) {
                iterator.remove();
                continue;
            }

            double phase = (60 - vortex.life) * 0.28D;
            for (int i = 0; i < 18; i++) {
                double angle = phase + i * (Math.PI * 2.0D / 18.0D);
                double radius = 0.6D + (i % 6) * 0.55D;
                double y = vortex.center.y + 0.15D + (i % 9) * 0.28D;
                world.spawnParticle(i % 3 == 0 ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.CLOUD,
                        vortex.center.x + Math.cos(angle) * radius,
                        y,
                        vortex.center.z + Math.sin(angle) * radius,
                        1, 0.0D, 0.03D, 0.0D, 0.01D);
            }

            AxisAlignedBB box = new AxisAlignedBB(
                    vortex.center.x - 5.0D, vortex.center.y - 2.0D, vortex.center.z - 5.0D,
                    vortex.center.x + 5.0D, vortex.center.y + 7.0D, vortex.center.z + 5.0D);
            for (LivingEntity target : world.getEntitiesWithinAABB(LivingEntity.class, box,
                    e -> e.isAlive() && !e.getUniqueID().equals(vortex.owner))) {
                Vector3d horizontal = new Vector3d(
                        vortex.center.x - target.getPosX(), 0.0D,
                        vortex.center.z - target.getPosZ());
                double distance = Math.max(0.4D, horizontal.length());
                Vector3d inward = horizontal.normalize().scale(0.12D + 0.18D / distance);
                Vector3d tangent = new Vector3d(-horizontal.z, 0.0D, horizontal.x)
                        .normalize().scale(0.18D);
                double lift = target.isOnGround() ? 0.42D : 0.12D;
                target.setMotion(target.getMotion().scale(0.82D)
                        .add(inward.x + tangent.x, lift, inward.z + tangent.z));
                target.velocityChanged = true;
                target.fallDistance = 0.0F;
            }
        }
    }
}
