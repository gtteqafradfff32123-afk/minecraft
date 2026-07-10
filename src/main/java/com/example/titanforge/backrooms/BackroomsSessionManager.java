package com.example.titanforge.backrooms;

import com.example.titanforge.TitanForge;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class BackroomsSessionManager {
    private static final Map<UUID, BackroomsSession> SESSIONS = new HashMap<>();

    private BackroomsSessionManager() {}

    public static boolean isInside(UUID playerId) {
        return SESSIONS.containsKey(playerId);
    }

    public static BackroomsSession get(UUID playerId) {
        return SESSIONS.get(playerId);
    }

    public static boolean start(ServerPlayerEntity victim, ServerPlayerEntity owner) {
        UUID id = victim.getUniqueID();
        if (SESSIONS.containsKey(id)) return false;
        ServerWorld overworld = victim.getServer().getWorld(net.minecraft.world.World.OVERWORLD);
        BlockPos center = victim.getPosition();
        long seed = victim.world.rand.nextLong();
        BackroomsSession session = new BackroomsSession(id, center, seed);
        SESSIONS.put(id, session);
        TitanForge.LOGGER.info("[backrooms] session started for {}", victim.getName().getString());
        return true;
    }

    public static void tick(ServerWorld world) {
        Iterator<Map.Entry<UUID, BackroomsSession>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, BackroomsSession> entry = it.next();
            BackroomsSession s = entry.getValue();
            if (s.finished) {
                it.remove();
                continue;
            }
            ServerPlayerEntity player = world.getServer().getPlayerList().getPlayerByUUID(s.playerId);
            if (player == null || !player.isAlive()) {
                it.remove();
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
        TitanForge.LOGGER.info("[backrooms] session finished for {}", playerId);
    }
}
