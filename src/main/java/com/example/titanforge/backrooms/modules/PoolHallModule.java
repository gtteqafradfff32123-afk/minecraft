package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class PoolHallModule extends BackroomsModule {
    public PoolHallModule(Direction f) {
        super(f, 20, 14, 6);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.POOL_HALL;
    }

    @Override
    protected void design() {
        shell(Blocks.QUARTZ_BRICKS.getDefaultState(),
                Blocks.SMOOTH_QUARTZ.getDefaultState(),
                Blocks.SEA_LANTERN.getDefaultState());
        for (int x = 2; x < 18; x++)
            for (int z = 2; z < 12; z++) {
                block(x, 1, z, Blocks.WATER.getDefaultState());
                block(x, 0, z, Blocks.PRISMARINE_BRICKS.getDefaultState());
            }
        for (int x = 1; x < 19; x += 4)
            block(x, 4, 7, Blocks.SEA_LANTERN.getDefaultState());
        clearDoor(10, 1, 0);
        clearDoor(10, 1, 13);
        socket(10, 1, 0, Direction.NORTH);
        socket(10, 1, 13, Direction.SOUTH);
    }

    private void shell(BlockState wall, BlockState floor, BlockState ceiling) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                block(x, 0, z, floor);
                block(x, height - 1, z, ceiling);
            }
        }
        for (int y = 1; y < height - 1; y++) {
            for (int x = 0; x < width; x++) {
                block(x, y, 0, wall);
                block(x, y, length - 1, wall);
            }
            for (int z = 1; z < length - 1; z++) {
                block(0, y, z, wall);
                block(width - 1, y, z, wall);
            }
        }
    }
}
