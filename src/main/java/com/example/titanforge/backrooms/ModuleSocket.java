package com.example.titanforge.backrooms;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public final class ModuleSocket {
    public final BlockPos pos;
    public final Direction facing;

    public ModuleSocket(BlockPos pos, Direction facing) {
        this.pos = pos.toImmutable();
        this.facing = facing;
    }
}
