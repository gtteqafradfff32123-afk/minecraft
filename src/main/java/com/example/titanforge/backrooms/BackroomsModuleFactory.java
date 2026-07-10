package com.example.titanforge.backrooms;

import com.example.titanforge.backrooms.modules.*;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public final class BackroomsModuleFactory {
    private BackroomsModuleFactory() {}

    public static BackroomsModule create(BackroomsModuleType t, BlockPos p, Direction f) {
        switch (t) {
            case LOOP_CORRIDOR: return new LoopCorridorModule(p, f);
            case EMPTY_OFFICE: return new EmptyOfficeModule(p, f);
            case FALSE_EXIT: return new FalseExitModule(p, f);
            case POOL_HALL: return new PoolHallModule(p, f);
            case SERVICE_CORRIDOR: return new ServiceCorridorModule(p, f);
            case ENDLESS_STAIR: return new EndlessStairModule(p, f);
            case QUIET_ROOM: return new QuietRoomModule(p, f);
            case VOID_SLICE: return new VoidSliceModule(p, f);
            case FOREIGN_SAVE: return new ForeignSaveModule(p, f);
            case DEAD_END: return new DeadEndModule(p, f);
            default: return new LobbyModule(p, f);
        }
    }

    private static void registerSpecial(BackroomsSession s, BackroomsModule m) {
        if (m.type() == BackroomsModuleType.FALSE_EXIT)
            for (ModuleSocket x : m.sockets()) FalseExitController.register(s, x.pos);
        if (m.type() == BackroomsModuleType.LOOP_CORRIDOR)
            for (ModuleSocket x : m.sockets()) LoopDoorController.register(s, x.pos);
        if (m.type() == BackroomsModuleType.EMPTY_OFFICE || m.type() == BackroomsModuleType.DEAD_END)
            LayoutErrorManager.register(s, new BlockPos(m.bounds().getCenter()));
    }
}
