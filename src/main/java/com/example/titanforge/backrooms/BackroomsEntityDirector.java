package com.example.titanforge.backrooms;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

public final class BackroomsEntityDirector {
    private BackroomsEntityDirector() {}

    public static void tick(ServerWorld w, ServerPlayerEntity p, BackroomsSession s) {
        if (s.pressure < 60 || s.ticks % 400 != 0) return;
        if (w.getEntitiesWithinAABB(EndermanEntity.class, p.getBoundingBox().grow(32)).size() >= 2) return;
        EndermanEntity e = EntityType.ENDERMAN.create(w);
        if (e == null) return;
        double a = (s.seed ^ s.ticks) * .013;
        BlockPos q = new BlockPos(p.getPosX() + Math.cos(a) * 18, p.getPosY(), p.getPosZ() + Math.sin(a) * 18);
        e.setPosition(q.getX() + .5, q.getY(), q.getZ() + .5);
        e.getPersistentData().putBoolean("TF_BackroomsShadow", true);
        w.addEntity(e);
    }
}
