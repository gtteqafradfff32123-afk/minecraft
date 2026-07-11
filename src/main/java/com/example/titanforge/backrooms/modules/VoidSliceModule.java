package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class VoidSliceModule extends BackroomsModule {
    public VoidSliceModule(Direction f) {
        super(f, 5, 18, 8);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.VOID_SLICE;
    }

    @Override
    protected void design() {
        shell(Blocks.BLACK_CONCRETE.getDefaultState(),
                Blocks.OBSIDIAN.getDefaultState(),
                Blocks.BLACK_CONCRETE.getDefaultState());
        for (int y = 1; y < 7; y++)
            for (int z = 2; z < 16; z++)
                block(2, y, z, Blocks.AIR.getDefaultState());
        clearDoor(2, 1, 0);
        socket(2, 1, 0, Direction.NORTH);
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
