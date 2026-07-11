package com.example.titanforge.backrooms;

import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public final class ModuleSocket {
    public final BlockPos localPos;
    public final Direction facing;

    public ModuleSocket(
            BlockPos localPos,
            Direction facing) {
        this.localPos = localPos.toImmutable();
        this.facing = facing;
    }

    public WorldSocket toWorld(
            BlockPos center,
            Direction moduleFacing) {
        return new WorldSocket(
                center.add(
                        BackroomsModule.rotateLocal(
                                localPos,
                                moduleFacing)),
                BackroomsModule.rotateDirection(
                        facing,
                        moduleFacing));
    }
}
