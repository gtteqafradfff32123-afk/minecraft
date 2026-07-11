package com.example.titanforge.backrooms;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public final class WorldSocket {
    public final BlockPos worldPos;
    public final Direction facing;

    public WorldSocket(
            BlockPos worldPos,
            Direction facing) {
        this.worldPos = worldPos.toImmutable();
        this.facing = facing;
    }
}
