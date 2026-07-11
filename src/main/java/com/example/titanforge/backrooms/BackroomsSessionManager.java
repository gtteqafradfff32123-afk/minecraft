package com.example.titanforge.backrooms;

import com.example.titanforge.TitanForge;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public final class BackroomsSessionManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final int MAX_SESSIONS = 16;
    private static final Map<UUID, BackroomsSession> ACTIVE = new HashMap<>();
    private static final Map<UUID, BackroomsGenerator> GENERATORS = new HashMap<>();
    private static final Set<Integer> USED_SLOTS = new HashSet<>();

    private BackroomsSessionManager() {}

    public static boolean start(ServerPlayerEntity target, ServerPlayerEntity owner) {
        if (!canStart(target)) return false;

        ServerWorld destination = BackroomsDimension.get(target.getServer());
        if (destination == null) return false;

        int slot = findFreeSlot();
        BlockPos center = new BlockPos(slot * 512, 64, 0);

        BackroomsSession session = new BackroomsSession(
                target.getUniqueID(),
                owner.getUniqueID(),
                target.world.getDimensionKey(),
                target.getPosition(),
                target.world.rand.nextLong(),
                center,
                slot);

        BackroomsGenerator generator = new BackroomsGenerator(session);

        ACTIVE.put(target.getUniqueID(), session);
        GENERATORS.put(target.getUniqueID(), generator);
        USED_SLOTS.add(slot);

        generator.start();
        if (!generator.buildInitialRoom(destination)) {
            ACTIVE.remove(target.getUniqueID());
            GENERATORS.remove(target.getUniqueID());
            USED_SLOTS.remove(slot);
            return false;
        }

        generator.ensureAhead(6);

        destination.getChunkProvider().registerTicket(
                TicketType.POST_TELEPORT,
                new ChunkPos(center),
                2,
                target.getEntityId());

        target.teleport(
                destination,
                center.getX() + 0.5D,
                center.getY() + 1.0D,
                center.getZ() + 0.5D,
                target.rotationYaw,
                target.rotationPitch);

        sync(target, session);
        return true;
    }

    public static void tick(ServerWorld world) {
        if (!BackroomsDimension.WORLD.equals(world.getDimensionKey())) return;

        for (BackroomsSession session : new ArrayList<>(ACTIVE.values())) {
            ServerPlayerEntity player = world.getServer().getPlayerList().getPlayerByUUID(session.playerId);
            if (player == null) continue;

            session.ticks++;

            BackroomsGenerator generator = GENERATORS.get(session.playerId);
            if (generator != null) {
                generator.ensureAhead(Math.min(24, 7 + session.roomsVisited));
                generator.tick(world);
            }

            BackroomsPressureManager.tick(world, player, session);
            LayoutErrorManager.tick(player, session);
            FalseExitController.tick(player, session);
            LoopDoorController.tick(player, session);
            FootprintTrailManager.tick(world, player, session);
            BackroomsEntityDirector.tick(world, player, session);
            ArchivistController.tick(world, player, session);

            if (session.errorsFound >= 3 && !session.bossStarted) {
                finish(player, true);
            } else if (session.ticks % 20 == 0) {
                sync(player, session);
            }
        }
    }

    public static void finish(ServerPlayerEntity player, boolean victory) {
        BackroomsSession session = ACTIVE.get(player.getUniqueID());
        if (session == null) return;

        session.finished = true;

        if (player.world.getDimensionKey().equals(BackroomsDimension.WORLD)) {
            RegistryKey<World> returnDim = session.returnDimension;
            BlockPos returnPos = session.returnPos;

            ServerWorld returnWorld = player.getServer().getWorld(returnDim);
            if (returnWorld == null) {
                returnWorld = player.getServer().getWorld(World.OVERWORLD);
                returnPos = returnWorld.getSpawnPoint();
            }

            player.teleport(returnWorld,
                    returnPos.getX() + 0.5D,
                    returnPos.getY() + 1.0D,
                    returnPos.getZ() + 0.5D,
                    player.rotationYaw,
                    player.rotationPitch);
        }

        cleanup(player);
    }

    public static void onPlayerDeath(ServerPlayerEntity player) {
        BackroomsSession session = ACTIVE.get(player.getUniqueID());
        if (session == null) return;

        session.finished = true;

        RegistryKey<World> returnDim = session.returnDimension;
        BlockPos returnPos = session.returnPos;

        ServerWorld returnWorld = player.getServer().getWorld(returnDim);
        if (returnWorld == null) {
            returnWorld = player.getServer().getWorld(World.OVERWORLD);
            returnPos = returnWorld.getSpawnPoint();
        }

        player.setPositionAndUpdate(
                returnPos.getX() + 0.5D,
                returnPos.getY() + 1.0D,
                returnPos.getZ() + 0.5D);

        cleanup(player);
    }

    public static void onLogout(UUID playerId) {
        BackroomsSession session = ACTIVE.get(playerId);
        if (session != null) {
            cleanupByUUID(playerId);
        }
    }

    public static boolean isInBackrooms(ServerPlayerEntity player) {
        return ACTIVE.containsKey(player.getUniqueID());
    }

    public static BackroomsSession getSession(UUID playerId) {
        return ACTIVE.get(playerId);
    }

    private static boolean canStart(ServerPlayerEntity target) {
        if (ACTIVE.containsKey(target.getUniqueID())) {
            target.sendStatusMessage(
                    new net.minecraft.util.text.StringTextComponent("§cТы уже в Backrooms"), true);
            return false;
        }
        if (ACTIVE.size() >= MAX_SESSIONS) {
            target.sendStatusMessage(
                    new net.minecraft.util.text.StringTextComponent("§cЛимит сессий Backrooms"), true);
            return false;
        }
        return true;
    }

    private static int findFreeSlot() {
        for (int i = 0; i < MAX_SESSIONS; i++) {
            if (!USED_SLOTS.contains(i)) return i;
        }
        return -1;
    }

    private static void sync(ServerPlayerEntity player, BackroomsSession session) {
        BackroomsSyncPacket packet = new BackroomsSyncPacket(
                session.pressure,
                session.errorsFound,
                session.building,
                session.finished);
        com.example.titanforge.NetworkHandler.sendTo(player, packet);
    }

    private static void cleanup(ServerPlayerEntity player) {
        UUID id = player.getUniqueID();
        LayoutErrorManager.clear(id);
        FalseExitController.clear(id);
        LoopDoorController.clear(id);
        player.getPersistentData().remove("TF_ArchivistSpawned");
        GENERATORS.remove(id);
        BackroomsSession session = ACTIVE.remove(id);
        if (session != null) {
            USED_SLOTS.remove(session.slot);
        }
    }

    private static void cleanupByUUID(UUID id) {
        LayoutErrorManager.clear(id);
        FalseExitController.clear(id);
        LoopDoorController.clear(id);
        GENERATORS.remove(id);
        BackroomsSession session = ACTIVE.remove(id);
        if (session != null) {
            USED_SLOTS.remove(session.slot);
        }
    }
}
