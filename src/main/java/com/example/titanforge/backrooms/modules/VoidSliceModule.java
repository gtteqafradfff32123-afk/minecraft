package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public final class VoidSliceModule extends BackroomsModule {
    public VoidSliceModule(BlockPos p, Direction f) { super(p, f, 5, 18, 8); }
    public BackroomsModuleType type() { return BackroomsModuleType.VOID_SLICE; }
    protected void design() {
        shell(Blocks.BLACK_CONCRETE.getDefaultState(), Blocks.OBSIDIAN.getDefaultState(), Blocks.BLACK_CONCRETE.getDefaultState());
        for (int y = 1; y < 7; y++) for (int z = 2; z < 16; z++) block(2, y, z, Blocks.AIR.getDefaultState());
        clearDoor(2, 1, 0); socket(2, 1, 0, Direction.NORTH);
    }
}
