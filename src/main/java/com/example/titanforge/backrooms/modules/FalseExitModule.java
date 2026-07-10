package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public final class FalseExitModule extends BackroomsModule {
    public FalseExitModule(BlockPos p, Direction f) { super(p, f, 9, 8, 5); }
    public BackroomsModuleType type() { return BackroomsModuleType.FALSE_EXIT; }
    protected void design() {
        shell(Blocks.SMOOTH_SANDSTONE.getDefaultState(), Blocks.YELLOW_WOOL.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState());
        for (int y = 1; y < 4; y++) for (int x = 3; x < 6; x++) block(x, y, 7, Blocks.BLACK_CONCRETE.getDefaultState());
        block(4, 1, 6, Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE.getDefaultState());
        clearDoor(4, 1, 0); socket(4, 1, 0, Direction.NORTH);
    }
}
