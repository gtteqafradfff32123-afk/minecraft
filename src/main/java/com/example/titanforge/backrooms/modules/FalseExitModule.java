package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class FalseExitModule extends BackroomsModule {
    public FalseExitModule(Direction f) {
        super(f, 9, 8, 5);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.FALSE_EXIT;
    }

    @Override
    protected void design() {
        shell(Blocks.SMOOTH_SANDSTONE.getDefaultState(),
                Blocks.YELLOW_WOOL.getDefaultState(),
                Blocks.SMOOTH_SANDSTONE.getDefaultState());
        for (int y = 1; y < 4; y++)
            for (int x = 3; x < 6; x++)
                block(x, y, 7, Blocks.BLACK_CONCRETE.getDefaultState());
        block(4, 1, 6, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.getDefaultState());
        clearDoor(4, 1, 0);
        socket(4, 1, 0, Direction.NORTH);
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
