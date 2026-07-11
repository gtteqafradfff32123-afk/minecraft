package com.example.titanforge.backrooms;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public final class LayoutErrorManager {
    private static final Map<UUID, Set<BlockPos>> ERRORS = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> FOUND = new HashMap<>();

    private LayoutErrorManager() {}

    public static void register(BackroomsSession s, BlockPos p) {
        ERRORS.computeIfAbsent(s.playerId, k -> new HashSet<>()).add(p.toImmutable());
    }

    public static void tick(ServerPlayerEntity p, BackroomsSession s) {
        if ((s.ticks % 5) != 0) return;
        Set<BlockPos> all = ERRORS.get(s.playerId);
        if (all == null) return;

        Set<BlockPos> found = FOUND.computeIfAbsent(s.playerId, k -> new HashSet<>());
        for (BlockPos pos : all) {
            if (!found.contains(pos) && p.getDistanceSq(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 6.25D) {
                found.add(pos);
                BackroomsPressureManager.foundError(s);
                p.sendStatusMessage(
                    new net.minecraft.util.text.StringTextComponent(
                        "§6Ошибка планировки " + s.errorsFound + "/3"), true);
            }
        }
    }

    public static void clear(UUID id) {
        ERRORS.remove(id);
        FOUND.remove(id);
    }
}
