package com.example.titanforge.backrooms;

import com.example.titanforge.TitanForge;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import java.util.Random;

public final class BackroomsGenerator {
    private static final int BLOCK_BUDGET = 180;
    private final BackroomsSession session;
    private final Random random;

    public BackroomsGenerator(
            BackroomsSession session) {
        this.session = session;
        this.random = new Random(session.seed);
    }

    public void start() {
        BackroomsModule lobby =
                BackroomsModuleFactory.create(
                        BackroomsModuleType.LOBBY,
                        Direction.NORTH);
        PlacedModule placed =
                new PlacedModule(
                        lobby,
                        session.center);
        session.modules.add(placed);
        session.buildQueue.add(placed);

        for (ModuleSocket socket : lobby.sockets()) {
            session.openSockets.add(
                    socket.toWorld(
                            session.center,
                            lobby.facing()));
        }
    }

    public boolean buildInitialRoom(
            ServerWorld world) {
        PlacedModule lobby = session.buildQueue.peek();
        if (lobby == null) return false;

        while (!lobby.module.buildStep(
                world,
                lobby.center,
                4096)) {
        }

        session.buildQueue.poll();
        registerSpecial(lobby);
        session.building =
                !session.buildQueue.isEmpty();
        return true;
    }

    public void ensureAhead(int desired) {
        int guard = 0;
        while (session.modules.size() < desired
                && !session.openSockets.isEmpty()
                && guard++ < 128) {
            WorldSocket socket =
                    session.openSockets.poll();
            PlacedModule placed = createAt(socket);
            if (placed == null) continue;

            session.modules.add(placed);
            session.buildQueue.add(placed);

            for (ModuleSocket local
                    : placed.module.sockets()) {
                WorldSocket next =
                        local.toWorld(
                                placed.center,
                                placed.module.facing());
                if (!next.worldPos.withinDistance(
                        socket.worldPos,
                        3.0D)) {
                    session.openSockets.add(next);
                }
            }
        }
        session.building =
                !session.buildQueue.isEmpty();
    }

    public void tick(ServerWorld world) {
        PlacedModule placed =
                session.buildQueue.peek();
        if (placed == null) {
            session.building = false;
            return;
        }

        if (placed.module.buildStep(
                world,
                placed.center,
                BLOCK_BUDGET)) {
            session.buildQueue.poll();
            registerSpecial(placed);
            TitanForge.LOGGER.info(
                    "Backrooms placed {} at {} sockets={} queue={}",
                    placed.module.type(),
                    placed.center,
                    placed.module.sockets().size(),
                    session.buildQueue.size());
        }
        session.building =
                !session.buildQueue.isEmpty();
    }

    private PlacedModule createAt(
            WorldSocket socket) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BackroomsModule candidate =
                    BackroomsModuleFactory.create(
                            weightedType(),
                            socket.facing);
            BlockPos center =
                    socket.worldPos.offset(
                            socket.facing,
                            candidate.connectionOffset());
            if (!intersects(
                    candidate.boundsAt(center))) {
                return new PlacedModule(
                        candidate,
                        center);
            }
        }
        return null;
    }

    private boolean intersects(
            AxisAlignedBB bounds) {
        for (PlacedModule old : session.modules) {
            if (old.module
                    .boundsAt(old.center)
                    .grow(1.0D)
                    .intersects(bounds)) {
                return true;
            }
        }
        return false;
    }

    private BackroomsModuleType weightedType() {
        int total = 0;
        for (BackroomsModuleType type
                : BackroomsModuleType.values()) {
            if (type != BackroomsModuleType.LOBBY) {
                total += type.weight;
            }
        }

        int roll = random.nextInt(Math.max(1, total));
        for (BackroomsModuleType type
                : BackroomsModuleType.values()) {
            if (type == BackroomsModuleType.LOBBY) {
                continue;
            }
            roll -= type.weight;
            if (roll < 0) return type;
        }
        return BackroomsModuleType.LOOP_CORRIDOR;
    }

    private void registerSpecial(
            PlacedModule placed) {
        BackroomsModuleType type =
                placed.module.type();
        if (type == BackroomsModuleType.EMPTY_OFFICE
                || type == BackroomsModuleType.DEAD_END) {
            LayoutErrorManager.register(
                    session,
                    placed.center);
        }
        if (type == BackroomsModuleType.FALSE_EXIT) {
            FalseExitController.register(
                    session,
                    placed.center);
        }
        if (type == BackroomsModuleType.LOOP_CORRIDOR) {
            LoopDoorController.register(
                    session,
                    placed.center);
        }
    }
}
