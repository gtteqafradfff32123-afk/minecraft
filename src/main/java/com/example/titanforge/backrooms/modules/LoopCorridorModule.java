package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class LoopCorridorModule extends BackroomsModule {
    public LoopCorridorModule(Direction f) { super(f, 16, 7, 5); }
    public BackroomsModuleType type() { return BackroomsModuleType.LOOP_CORRIDOR; }
    protected void design() {
        shell(Blocks.YELLOW_WOOL.getDefaultState(), Blocks.YELLOW_WOOL.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState());
        clearDoor(7, 1, 0); clearDoor(7, 1, 6); socket(7, 1, 0, Direction.NORTH); socket(7, 1, 6, Direction.SOUTH);
    }
}
