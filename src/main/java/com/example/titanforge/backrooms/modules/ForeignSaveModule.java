package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public final class ForeignSaveModule extends BackroomsModule {
    public ForeignSaveModule(BlockPos p, Direction f) { super(p, f, 15, 12, 6); }
    public BackroomsModuleType type() { return BackroomsModuleType.FOREIGN_SAVE; }
    protected void design() {
        shell(Blocks.OAK_PLANKS.getDefaultState(), Blocks.GRASS_BLOCK.getDefaultState(), Blocks.GLASS.getDefaultState());
        for (int x = 3; x < 12; x++) for (int z = 3; z < 9; z++) if ((x + z) % 4 == 0) block(x, 1, z, Blocks.COBWEB.getDefaultState());
        block(7, 1, 6, Blocks.CHEST.getDefaultState()); clearDoor(7, 1, 0); clearDoor(7, 1, 11); socket(7, 1, 0, Direction.NORTH); socket(7, 1, 11, Direction.SOUTH);
    }
}
