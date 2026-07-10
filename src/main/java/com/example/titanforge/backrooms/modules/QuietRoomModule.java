package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public final class QuietRoomModule extends BackroomsModule {
    public QuietRoomModule(BlockPos p, Direction f) { super(p, f, 10, 9, 5); }
    public BackroomsModuleType type() { return BackroomsModuleType.QUIET_ROOM; }
    protected void design() {
        shell(Blocks.LIME_TERRACOTTA.getDefaultState(), Blocks.GREEN_WOOL.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState());
        for (int x = 2; x < 8; x++) for (int z = 2; z < 7; z++) block(x, 4, z, Blocks.SEA_LANTERN.getDefaultState());
        block(5, 1, 4, Blocks.WHITE_BED.getDefaultState()); clearDoor(5, 1, 0); clearDoor(5, 1, 8); socket(5, 1, 0, Direction.NORTH); socket(5, 1, 8, Direction.SOUTH);
    }
}
