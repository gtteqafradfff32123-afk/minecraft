package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class DeadEndModule extends BackroomsModule {
    public DeadEndModule(Direction f) {
        super(f, 7, 9, 5);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.DEAD_END;
    }

    @Override
    protected void design() {
        shell(Blocks.YELLOW_TERRACOTTA.getDefaultState(),
                Blocks.YELLOW_WOOL.getDefaultState(),
                Blocks.SMOOTH_SANDSTONE.getDefaultState());
        block(3, 1, 7, Blocks.REDSTONE_TORCH.getDefaultState());
        clearDoor(3, 1, 0);
        socket(3, 1, 0, Direction.NORTH);
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
