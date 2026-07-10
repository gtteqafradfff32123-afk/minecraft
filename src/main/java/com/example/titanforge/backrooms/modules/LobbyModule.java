package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class LobbyModule extends BackroomsModule {
    public LobbyModule(Direction f) { super(f, 14, 11, 5); }
    public BackroomsModuleType type() { return BackroomsModuleType.LOBBY; }
    protected void design() {
        shell(Blocks.YELLOW_WOOL.getDefaultState(), Blocks.YELLOW_WOOL.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState());
        block(5, 1, 3, Blocks.GLOWSTONE.getDefaultState());
        clearDoor(6, 1, 0); clearDoor(6, 1, 10); socket(6, 1, 0, Direction.NORTH); socket(6, 1, 10, Direction.SOUTH);
    }
}
