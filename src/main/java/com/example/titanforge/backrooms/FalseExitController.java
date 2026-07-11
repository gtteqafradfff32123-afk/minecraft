package com.example.titanforge.backrooms;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public final class FalseExitController {
    private static final Map<UUID, Set<BlockPos>> DOORS = new HashMap<>();
    private static final Map<UUID, Long> GUARD = new HashMap<>();

    public static void register(BackroomsSession s, BlockPos p) {
        DOORS.computeIfAbsent(s.playerId, k -> new HashSet<>()).add(p.toImmutable());
    }

    public static void tick(ServerPlayerEntity p, BackroomsSession s) {
        long now = p.world.getGameTime();
        if (GUARD.getOrDefault(p.getUniqueID(), 0L) > now) return;
        Set<BlockPos> doors = DOORS.get(p.getUniqueID());
        if (doors == null) return;

        for (BlockPos d : doors) {
            if (p.getDistanceSq(d.getX() + .5, d.getY() + .5, d.getZ() + .5) < 1.5D) {
                GUARD.put(p.getUniqueID(), now + 40);
                Random r = new Random(s.seed ^ now);
                BlockPos dst = s.center.add(r.nextInt(101) - 50, 1, r.nextInt(101) - 50);
                p.setPositionAndUpdate(dst.getX() + .5, dst.getY(), dst.getZ() + .5);
                BackroomsPressureManager.add(s, 10);
                break;
            }
        }
    }

    public static void clear(UUID id) {
        DOORS.remove(id);
        GUARD.remove(id);
    }
}
