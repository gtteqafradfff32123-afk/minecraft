package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class ServiceCorridorModule extends BackroomsModule {
    public ServiceCorridorModule(Direction f) { super(f, 22, 6, 5); }
    public BackroomsModuleType type() { return BackroomsModuleType.SERVICE_CORRIDOR; }
    protected void design() {
        shell(Blocks.STONE_BRICKS.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState());
        clearDoor(10, 1, 0); clearDoor(10, 1, 5); socket(10, 1, 0, Direction.NORTH); socket(10, 1, 5, Direction.SOUTH);
    }
}
