package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.BackroomsModule;
import com.example.titanforge.backrooms.BackroomsModuleType;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class DeadEndModule extends BackroomsModule {
    public DeadEndModule(Direction f) { super(f, 7, 9, 5); }
    public BackroomsModuleType type() { return BackroomsModuleType.DEAD_END; }
    protected void design() {
        shell(Blocks.YELLOW_TERRACOTTA.getDefaultState(), Blocks.YELLOW_WOOL.getDefaultState(), Blocks.SMOOTH_SANDSTONE.getDefaultState());
        block(3, 1, 7, Blocks.REDSTONE_TORCH.getDefaultState()); clearDoor(3, 1, 0); socket(3, 1, 0, Direction.NORTH);
    }
}
