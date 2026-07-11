package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class QuietRoomModule extends BackroomsModule {
    public QuietRoomModule(Direction f) {
        super(f, 10, 9, 5);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.QUIET_ROOM;
    }

    @Override
    protected void design() {
        shell(Blocks.LIME_TERRACOTTA.getDefaultState(),
                Blocks.GREEN_WOOL.getDefaultState(),
                Blocks.SMOOTH_SANDSTONE.getDefaultState());
        for (int x = 2; x < 8; x++)
            for (int z = 2; z < 7; z++)
                block(x, 4, z, Blocks.SEA_LANTERN.getDefaultState());
        block(5, 1, 4, Blocks.WHITE_BED.getDefaultState());
        clearDoor(5, 1, 0);
        clearDoor(5, 1, 8);
        socket(5, 1, 0, Direction.NORTH);
        socket(5, 1, 8, Direction.SOUTH);
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
