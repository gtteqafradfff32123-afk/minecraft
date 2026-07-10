package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public final class PoolHallModule extends BackroomsModule {
    public PoolHallModule(BlockPos p, Direction f) { super(p, f, 20, 14, 6); }
    public BackroomsModuleType type() { return BackroomsModuleType.POOL_HALL; }
    protected void design() {
        shell(Blocks.QUARTZ_BRICKS.getDefaultState(), Blocks.SMOOTH_QUARTZ.getDefaultState(), Blocks.SEA_LANTERN.getDefaultState());
        for (int x = 2; x < 18; x++) for (int z = 2; z < 12; z++) {
            block(x, 1, z, Blocks.WATER.getDefaultState());
            block(x, 0, z, Blocks.PRISMARINE_BRICKS.getDefaultState());
        }
        for (int x = 1; x < 19; x += 4) block(x, 4, 7, Blocks.SEA_LANTERN.getDefaultState());
        clearDoor(10, 1, 0); clearDoor(10, 1, 13); socket(10, 1, 0, Direction.NORTH); socket(10, 1, 13, Direction.SOUTH);
    }
}
