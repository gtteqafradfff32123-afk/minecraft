package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class LoopCorridorModule extends BackroomsModule {
    public LoopCorridorModule(Direction facing) {
        super(facing, 5, 14, 6);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.LOOP_CORRIDOR;
    }

    @Override
    protected void design() {
        standardShell();

        block(2, 1, 4, Blocks.STRIPPED_OAK_LOG.getDefaultState());
        block(2, 2, 4, Blocks.STRIPPED_OAK_LOG.getDefaultState());
        block(2, 1, 9, Blocks.STRIPPED_OAK_LOG.getDefaultState());
        block(2, 2, 9, Blocks.STRIPPED_OAK_LOG.getDefaultState());

        clearDoor(1, 1, 0);
        clearDoor(1, 1, length - 1);
        socket(2, 1, 0, Direction.NORTH);
        socket(2, 1, length - 1, Direction.SOUTH);
    }
}
