package com.example.titanforge.backrooms;

import com.example.titanforge.TitanForge;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class BackroomsSessionManager {
    private static final Map<UUID, BackroomsSession> SESSIONS = new HashMap<>();
    private static final Map<UUID, BackroomsGenerator> GENERATORS = new HashMap<>();

    private BackroomsSessionManager() {}

    public static boolean isInside(UUID playerId) {
        return SESSIONS.containsKey(playerId);
    }

    public static BackroomsSession get(UUID playerId) {
        return SESSIONS.get(playerId);
    }

    private static boolean canStart(ServerPlayerEntity target) {
        return !SESSIONS.containsKey(target.getUniqueID());
    }

    private static int findFreeSlot() {
        boolean[] used = new boolean[256];
        for (BackroomsSession s : SESSIONS.values()) {
            int slot = s.center.getX() / 512;
            if (slot >= 0 && slot < used.length) used[slot] = true;
        }
        for (int i = 0; i < used.length; i++) {
            if (!used[i]) return i;
        }
        return SESSIONS.size();
    }

    public static boolean start(ServerPlayerEntity target, ServerPlayerEntity owner) {
        if (!canStart(target)) return false;

        ServerWorld destination = BackroomsDimension.get(target.getServer());
        if (destination == null) {
            target.sendStatusMessage(new StringTextComponent("§cИзмерение Backrooms не загружено (нужен новый мир или datapack)."), true);
            TitanForge.LOGGER.warn("[backrooms] dimension not available for {}", target.getName().getString());
            return false;
        }

        int slot = findFreeSlot();
        BlockPos center = new BlockPos(slot * 512, 64, 0);

        BackroomsSession session = new BackroomsSession(
            target.getUniqueID(),
            owner.getUniqueID(),
            target.world.getDimensionKey(),
            target.getPosition(),
            target.world.rand.nextLong(),
            center);

        BackroomsGenerator generator = new BackroomsGenerator(session);

        SESSIONS.put(target.getUniqueID(), session);
        GENERATORS.put(target.getUniqueID(), generator);

        generator.start();
        generator.ensureAhead(6);
        generator.buildInitialRoom(destination);

        destination.getChunkProvider().registerTicket(
            TicketType.POST_TELEPORT,
            new ChunkPos(center),
            2,
            target.getEntityId());

        target.teleport(
            destination,
            center.getX() + 0.5,
            center.getY() + 1.0,
            center.getZ() + 0.5,
            target.rotationYaw,
            target.rotationPitch);

        target.sendStatusMessage(new StringTextComponent("§8Ты вошёл в Backrooms. Не доверяй выходам."), false);
        TitanForge.LOGGER.info("[backrooms] session started for {} slot={}", target.getName().getString(), slot);
        return true;
    }

    public static void tick(ServerWorld world) {
        Iterator<Map.Entry<UUID, BackroomsGenerator>> git = GENERATORS.entrySet().iterator();
        while (git.hasNext()) {
            Map.Entry<UUID, BackroomsGenerator> ge = git.next();
            BackroomsSession s = SESSIONS.get(ge.getKey());
            if (s == null || s.finished) {
                git.remove();
                continue;
            }
            ge.getValue().tick(world);
        }

        Iterator<Map.Entry<UUID, BackroomsSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BackroomsSession> entry = it.next();
            BackroomsSession s = entry.getValue();
            if (s.finished) {
                it.remove();
                GENERATORS.remove(entry.getKey());
                continue;
            }
            ServerPlayerEntity player = world.getServer().getPlayerList().getPlayerByUUID(s.playerId);
            if (player == null || !player.isAlive()) {
                it.remove();
                GENERATORS.remove(entry.getKey());
                continue;
            }
            s.ticks++;
            BackroomsPressureManager.tick(world, player, s);
            LayoutErrorManager.tick(player, s);
            FalseExitController.tick(player, s);
            LoopDoorController.tick(player, s);
            FootprintTrailManager.tick(world, player, s);
            BackroomsEntityDirector.tick(world, player, s);
            ArchivistController.tick(world, player, s);
        }
    }

    public static void finish(UUID playerId) {
        BackroomsSession s = SESSIONS.get(playerId);
        if (s == null) return;
        s.finished = true;
        LayoutErrorManager.clear(playerId);
        FalseExitController.clear(playerId);
        LoopDoorController.clear(playerId);
        ServerPlayerEntity player = null;
        for (ServerWorld w : net.minecraftforge.fml.server.ServerLifecycleHooks.getCurrentServer().getWorlds()) {
            player = w.getServer().getPlayerList().getPlayerByUUID(playerId);
            if (player != null) break;
        }
        if (player != null) {
            player.getPersistentData().remove("TF_ArchivistSpawned");
        }
        SESSIONS.remove(playerId);
        GENERATORS.remove(playerId);
        TitanForge.LOGGER.info("[backrooms] session finished for {}", playerId);
    }
}
