package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class EmptyOfficeModule extends BackroomsModule {
    public EmptyOfficeModule(Direction facing) {
        super(facing, 12, 9, 6);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.EMPTY_OFFICE;
    }

    @Override
    protected void design() {
        standardShell();

        block(3, 1, 4, Blocks.OAK_PLANKS.getDefaultState());
        block(4, 1, 4, Blocks.OAK_PLANKS.getDefaultState());
        block(3, 2, 4, Blocks.OAK_PLANKS.getDefaultState());
        block(8, 1, 4, Blocks.OAK_FENCE.getDefaultState());
        block(8, 2, 4, Blocks.OAK_FENCE.getDefaultState());

        clearDoor(width / 2, 1, 0);
        clearDoor(width / 2, 1, length - 1);
        socket(width / 2 + 1, 1, 0, Direction.NORTH);
        socket(width / 2 + 1, 1, length - 1, Direction.SOUTH);
    }
}
