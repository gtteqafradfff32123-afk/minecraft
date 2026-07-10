package com.example.titanforge.backrooms;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public final class LoopDoorController {
    private static final Map<UUID, Map<BlockPos, Integer>> PASSES = new HashMap<>();
    private static final Map<UUID, Long> GUARD = new HashMap<>();

    private LoopDoorController() {}

    public static void register(BackroomsSession s, BlockPos p) {
        PASSES.computeIfAbsent(s.playerId, k -> new HashMap<>()).put(p.toImmutable(), 0);
    }

    public static void tick(ServerPlayerEntity p, BackroomsSession s) {
        long now = p.world.getGameTime();
        if (GUARD.getOrDefault(p.getUniqueID(), 0L) > now) return;
        Map<BlockPos, Integer> map = PASSES.get(p.getUniqueID());
        if (map == null) return;
        for (Map.Entry<BlockPos, Integer> e : map.entrySet())
            if (p.getDistanceSq(e.getKey().getX() + .5, e.getKey().getY() + .5, e.getKey().getZ() + .5) < 1.2D) {
                int n = e.getValue() + 1;
                e.setValue(n);
                GUARD.put(p.getUniqueID(), now + 30);
                if (n % 3 == 0) {
                    BlockPos q = e.getKey().add(0, 0, -8);
                    p.setPositionAndUpdate(q.getX() + .5, q.getY(), q.getZ() + .5);
                    BackroomsPressureManager.add(s, 4);
                }
                break;
            }
    }

    public static void clear(UUID id) {
        PASSES.remove(id);
        GUARD.remove(id);
    }
}
