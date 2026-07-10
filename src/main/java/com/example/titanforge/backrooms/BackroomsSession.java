package com.example.titanforge.backrooms;

import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public final class BackroomsSession {
    public final UUID playerId;
    public final BlockPos center;
    public int ticks = 0;
    public int pressure = 0;
    public int errorsFound = 0;
    public long seed;
    public boolean bossStarted = false;
    public boolean finished = false;

    public BackroomsSession(UUID playerId, BlockPos center, long seed) {
        this.playerId = playerId;
        this.center = center.toImmutable();
        this.seed = seed;
    }
}
