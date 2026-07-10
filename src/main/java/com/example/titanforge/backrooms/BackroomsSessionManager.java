package com.example.titanforge.backrooms;

import com.example.titanforge.TitanForge;
import com.example.titanforge.liminal.LiminalDimension;
import com.example.titanforge.liminal.LiminalManager;
import com.example.titanforge.liminal.copy.DeltaCopier;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.StringTextComponent;
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
        ServerWorld backroomsWorld = LiminalDimension.get(victim.getServer());
        if (backroomsWorld == null) {
            victim.sendStatusMessage(new StringTextComponent("§cИзмерение Backrooms не загружено (нужен новый мир или datapack)."), true);
            TitanForge.LOGGER.warn("[backrooms] dimension not available for {}", victim.getName().getString());
            return false;
        }
        BlockPos center = victim.getPosition();
        long seed = victim.world.rand.nextLong();
        BackroomsSession session = new BackroomsSession(id, center, seed);
        SESSIONS.put(id, session);

        // Build floor and wall to prevent falling into void
        int radius = 100;
        int floorY = Math.max(0, center.getY() - 41);
        DeltaCopier.buildFloor(backroomsWorld, center, radius);
        LiminalManager.buildVoidWall(backroomsWorld, center, radius);

        TitanForge.LOGGER.info("[backrooms] teleporting player={} to center={} floorY={}", victim.getName().getString(), center, floorY);
        victim.teleport(backroomsWorld, center.getX() + 0.5, floorY + 2, center.getZ() + 0.5, victim.rotationYaw, victim.rotationPitch);
        victim.addPotionEffect(new EffectInstance(Effects.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        victim.addPotionEffect(new EffectInstance(Effects.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
        victim.addPotionEffect(new EffectInstance(Effects.JUMP_BOOST, Integer.MAX_VALUE, 128, false, false));
        victim.sendStatusMessage(new StringTextComponent("§8Ты вошёл в Backrooms. Не доверяй выходам."), false);
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
