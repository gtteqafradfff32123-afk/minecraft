package com.example.titanforge.backrooms;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;

public final class BackroomsSession {
    public final UUID playerId;
    public final UUID ownerId;
    public final RegistryKey<World> returnDimension;
    public final BlockPos returnPos;
    public final BlockPos center;
    public final Queue<BackroomsModule> buildQueue = new LinkedList<>();
    public int ticks = 0;
    public int pressure = 0;
    public int errorsFound = 0;
    public long seed;
    public boolean bossStarted = false;
    public boolean finished = false;
    public boolean building = false;

    public BackroomsSession(UUID playerId, UUID ownerId, RegistryKey<World> returnDimension, BlockPos returnPos, long seed, BlockPos center) {
        this.playerId = playerId;
        this.ownerId = ownerId;
        this.returnDimension = returnDimension;
        this.returnPos = returnPos.toImmutable();
        this.seed = seed;
        this.center = center.toImmutable();
    }
}
