package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;

public final class EndlessStairModule extends BackroomsModule {
    public EndlessStairModule(Direction f) {
        super(f, 13, 10, 10);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.ENDLESS_STAIR;
    }

    @Override
    protected void design() {
        shell(Blocks.YELLOW_TERRACOTTA.getDefaultState(),
                Blocks.STONE_BRICKS.getDefaultState(),
                Blocks.SMOOTH_SANDSTONE.getDefaultState());
        for (int z = 1; z < 9; z++) {
            int y = 1 + (z % 7);
            for (int x = 4; x < 9; x++)
                block(x, y, z,
                        Blocks.STONE_BRICK_STAIRS.getDefaultState()
                                .with(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        }
        clearDoor(6, 1, 0);
        clearDoor(6, 1, 9);
        socket(6, 1, 0, Direction.NORTH);
        socket(6, 1, 9, Direction.SOUTH);
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
