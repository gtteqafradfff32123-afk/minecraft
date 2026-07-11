package com.example.titanforge.backrooms;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import java.util.*;

public final class BackroomsSession {
    public final UUID playerId;
    public final UUID ownerId;
    public final RegistryKey<World> returnDimension;
    public final BlockPos returnPos;
    public final long seed;
    public final BlockPos center;
    public final int slot;

    public final Queue<PlacedModule> buildQueue =
            new ArrayDeque<>();
    public final Queue<WorldSocket> openSockets =
            new ArrayDeque<>();
    public final List<PlacedModule> modules =
            new ArrayList<>();

    public int pressure;
    public int errorsFound;
    public int ticks;
    public int roomsVisited;
    public boolean building = true;
    public boolean bossStarted;
    public boolean finished;

    public BackroomsSession(
            UUID playerId,
            UUID ownerId,
            RegistryKey<World> returnDimension,
            BlockPos returnPos,
            long seed,
            BlockPos center,
            int slot) {
        this.playerId = playerId;
        this.ownerId = ownerId;
        this.returnDimension = returnDimension;
        this.returnPos = returnPos.toImmutable();
        this.seed = seed;
        this.center = center.toImmutable();
        this.slot = slot;
    }
}
