package com.example.titanforge.backrooms;

import com.example.titanforge.backrooms.modules.*;
import net.minecraft.util.Direction;

public final class BackroomsModuleFactory {
    private BackroomsModuleFactory() {}

    public static BackroomsModule create(BackroomsModuleType t, Direction f) {
        switch (t) {
            case LOOP_CORRIDOR: return new LoopCorridorModule(f);
            case EMPTY_OFFICE: return new EmptyOfficeModule(f);
            case FALSE_EXIT: return new FalseExitModule(f);
            case POOL_HALL: return new PoolHallModule(f);
            case SERVICE_CORRIDOR: return new ServiceCorridorModule(f);
            case ENDLESS_STAIR: return new EndlessStairModule(f);
            case QUIET_ROOM: return new QuietRoomModule(f);
            case VOID_SLICE: return new VoidSliceModule(f);
            case FOREIGN_SAVE: return new ForeignSaveModule(f);
            case DEAD_END: return new DeadEndModule(f);
            default: return new LobbyModule(f);
        }
    }
}
