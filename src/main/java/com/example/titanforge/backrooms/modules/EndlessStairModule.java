package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class EndlessStairModule extends BackroomsModule {
    public EndlessStairModule(Direction f) { super(f, 13, 10, 10); }
    public BackroomsModuleType type() { return BackroomsModuleType.ENDLESS_STAIR; }
    protected void design() {
        shell(Blocks.YELLOW_TERRACOTTA.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState());
        for (int z = 1; z < 9; z++) {
            int y = 1 + (z % 7);
            for (int x = 4; x < 9; x++)
                block(x, y, z, Blocks.STONE_BRICK_STAIRS.getDefaultState().with(net.minecraft.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
        }
        clearDoor(6, 1, 0); clearDoor(6, 1, 9); socket(6, 1, 0, Direction.NORTH); socket(6, 1, 9, Direction.SOUTH);
    }
}
