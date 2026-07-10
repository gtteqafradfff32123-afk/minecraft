package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class EmptyOfficeModule extends BackroomsModule {
    public EmptyOfficeModule(Direction f) { super(f, 18, 13, 5); }
    public BackroomsModuleType type() { return BackroomsModuleType.EMPTY_OFFICE; }
    protected void design() {
        shell(Blocks.OAK_PLANKS.getDefaultState(), Blocks.OAK_PLANKS.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState());
        block(8, 3, 6, Blocks.GLOWSTONE.getDefaultState());
        clearDoor(8, 1, 0); clearDoor(8, 1, 12); socket(8, 1, 0, Direction.NORTH); socket(8, 1, 12, Direction.SOUTH);
    }
}
