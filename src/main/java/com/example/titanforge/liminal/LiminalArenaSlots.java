package com.example.titanforge.liminal;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class LiminalArenaSlots {
    private static final int SPACING = 512;
    private static final int GRID = 2048;
    private static final int Y = 96;

    public static BlockPos center(UUID playerId) {
        long mixed = playerId.getMostSignificantBits()
                ^ Long.rotateLeft(playerId.getLeastSignificantBits(), 21);
        int xSlot = (int) Math.floorMod(mixed, (long) GRID);
        int zSlot = (int) Math.floorMod(mixed >>> 21, (long) GRID);
        return new BlockPos((xSlot - GRID / 2) * SPACING,
                Y, (zSlot - GRID / 2) * SPACING);
    }

    private LiminalArenaSlots() {}
}
