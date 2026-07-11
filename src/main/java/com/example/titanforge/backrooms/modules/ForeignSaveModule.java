package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class ForeignSaveModule extends BackroomsModule {
    public ForeignSaveModule(Direction f) {
        super(f, 15, 12, 6);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.FOREIGN_SAVE;
    }

    @Override
    protected void design() {
        shell(Blocks.OAK_PLANKS.getDefaultState(),
                Blocks.GRASS_BLOCK.getDefaultState(),
                Blocks.GLASS.getDefaultState());
        for (int x = 3; x < 12; x++)
            for (int z = 3; z < 9; z++)
                if ((x + z) % 4 == 0)
                    block(x, 1, z, Blocks.COBWEB.getDefaultState());
        block(7, 1, 6, Blocks.CHEST.getDefaultState());
        clearDoor(7, 1, 0);
        clearDoor(7, 1, 11);
        socket(7, 1, 0, Direction.NORTH);
        socket(7, 1, 11, Direction.SOUTH);
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
