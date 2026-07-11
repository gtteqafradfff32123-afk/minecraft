package com.example.titanforge.backrooms;

import net.minecraft.util.math.BlockPos;

public final class PlacedModule {
    public final BackroomsModule module;
    public final BlockPos center;

    public PlacedModule(
            BackroomsModule module,
            BlockPos center) {
        this.module = module;
        this.center = center.toImmutable();
    }
}
