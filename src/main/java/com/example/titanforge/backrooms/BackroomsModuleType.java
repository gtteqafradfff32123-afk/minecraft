package com.example.titanforge.backrooms;

public enum BackroomsModuleType {
    LOBBY(0),
    LOOP_CORRIDOR(20),
    EMPTY_OFFICE(18),
    FALSE_EXIT(5),
    POOL_HALL(12),
    SERVICE_CORRIDOR(16),
    ENDLESS_STAIR(10),
    QUIET_ROOM(8),
    VOID_SLICE(6),
    FOREIGN_SAVE(7),
    DEAD_END(4);

    public final int weight;

    BackroomsModuleType(int weight) {
        this.weight = weight;
    }
}
