package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class ServiceCorridorModule extends BackroomsModule {
    public ServiceCorridorModule(Direction facing) {
        super(facing, 6, 14, 6);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.SERVICE_CORRIDOR;
    }

    @Override
    protected void design() {
        standardShell();

        block(1, 1, 3, Blocks.IRON_BARS.getDefaultState());
        block(1, 2, 3, Blocks.IRON_BARS.getDefaultState());
        block(4, 1, 3, Blocks.IRON_BARS.getDefaultState());
        block(4, 2, 3, Blocks.IRON_BARS.getDefaultState());
        block(2, 3, 1, Blocks.REDSTONE_LAMP.getDefaultState());

        clearDoor(2, 1, 0);
        clearDoor(2, 1, length - 1);
        socket(3, 1, 0, Direction.NORTH);
        socket(3, 1, length - 1, Direction.SOUTH);
    }
}
