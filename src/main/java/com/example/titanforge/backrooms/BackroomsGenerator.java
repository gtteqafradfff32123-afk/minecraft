package com.example.titanforge.backrooms;

import com.example.titanforge.backrooms.modules.LobbyModule;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.Random;

public final class BackroomsGenerator {
    private final BackroomsSession session;
    private final Random random;
    private int modulesPlaced = 0;

    private static final BackroomsModuleType[] TYPES = {
        BackroomsModuleType.LOOP_CORRIDOR,
        BackroomsModuleType.EMPTY_OFFICE,
        BackroomsModuleType.FALSE_EXIT,
        BackroomsModuleType.POOL_HALL,
        BackroomsModuleType.SERVICE_CORRIDOR,
        BackroomsModuleType.ENDLESS_STAIR,
        BackroomsModuleType.QUIET_ROOM,
        BackroomsModuleType.VOID_SLICE,
        BackroomsModuleType.FOREIGN_SAVE,
        BackroomsModuleType.DEAD_END
    };

    public BackroomsGenerator(BackroomsSession session) {
        this.session = session;
        this.random = new Random(session.seed);
    }

    public void start() {
        session.buildQueue.add(new LobbyModule(Direction.NORTH));
        session.building = true;
    }

    public void ensureAhead(int count) {
        while (session.buildQueue.size() < count) {
            BackroomsModuleType t = TYPES[random.nextInt(TYPES.length)];
            Direction f = Direction.values()[random.nextInt(4)];
            session.buildQueue.add(BackroomsModuleFactory.create(t, f));
        }
    }

    public void buildInitialRoom(ServerWorld world) {
        if (session.buildQueue.isEmpty()) return;
        BackroomsModule first = session.buildQueue.poll();
        first.place(world, session.center);
        registerSpecial(first, session.center);
        modulesPlaced++;
        session.building = !session.buildQueue.isEmpty();
    }

    public void tick(ServerWorld world) {
        if (!session.building || session.buildQueue.isEmpty()) return;
        BackroomsModule m = session.buildQueue.poll();
        modulesPlaced++;
        BlockPos pos = new BlockPos(
            session.center.getX() + modulesPlaced * 512,
            session.center.getY(),
            session.center.getZ()
        );
        m.place(world, pos);
        registerSpecial(m, pos);
        session.building = !session.buildQueue.isEmpty();
    }

    private void registerSpecial(BackroomsModule m, BlockPos center) {
        if (m.type() == BackroomsModuleType.FALSE_EXIT)
            for (ModuleSocket x : m.sockets()) FalseExitController.register(session, x.pos);
        if (m.type() == BackroomsModuleType.LOOP_CORRIDOR)
            for (ModuleSocket x : m.sockets()) LoopDoorController.register(session, x.pos);
        if (m.type() == BackroomsModuleType.EMPTY_OFFICE || m.type() == BackroomsModuleType.DEAD_END)
            LayoutErrorManager.register(session, center);
    }
}
