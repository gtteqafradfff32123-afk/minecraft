package com.example.titanforge.liminal;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldSavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ShadowMemoryData extends WorldSavedData {
    private static final String DATA_NAME = "titanforge_shadow_memory";
    private final Map<UUID, PlayerMemory> players = new HashMap<>();

    public ShadowMemoryData() {
        super(DATA_NAME);
    }

    public static ShadowMemoryData get(ServerWorld world) {
        ServerWorld overworld = world.getServer().getWorld(ServerWorld.OVERWORLD);
        if (overworld == null) overworld = world;
        return overworld.getSavedData().getOrCreate(
                ShadowMemoryData::new, DATA_NAME);
    }

    public PlayerMemory memory(UUID playerId) {
        return players.computeIfAbsent(playerId, id -> new PlayerMemory());
    }

    public void onEnter(UUID playerId, long gameTime) {
        PlayerMemory memory = memory(playerId);
        memory.sessions++;
        memory.lastSeenGameTime = gameTime;
        markDirty();
    }

    public void onFirstHit(UUID playerId, long gameTime) {
        PlayerMemory memory = memory(playerId);
        memory.sessionsWherePlayerAttacked++;
        memory.lastSeenGameTime = gameTime;
        markDirty();
    }

    public void onHit(UUID playerId, long gameTime) {
        PlayerMemory memory = memory(playerId);
        memory.totalHits++;
        memory.lastSeenGameTime = gameTime;
        markDirty();
    }

    public void onWin(UUID playerId, long gameTime) {
        PlayerMemory memory = memory(playerId);
        memory.playerWins++;
        memory.lastResult = "PLAYER_WIN";
        memory.lastSeenGameTime = gameTime;
        markDirty();
    }

    public void onLoss(UUID playerId, long gameTime) {
        PlayerMemory memory = memory(playerId);
        memory.playerLosses++;
        memory.lastResult = "PLAYER_LOSS";
        memory.lastSeenGameTime = gameTime;
        markDirty();
    }

    public void onEscape(UUID playerId, long gameTime) {
        PlayerMemory memory = memory(playerId);
        memory.escapes++;
        memory.lastResult = "ESCAPE";
        memory.lastSeenGameTime = gameTime;
        markDirty();
    }

    public void rememberMessage(UUID playerId, String message) {
        PlayerMemory memory = memory(playerId);
        if (message != null) {
            String clean = message.replace('\n', ' ').trim();
            memory.lastPlayerMessage =
                    clean.substring(0, Math.min(clean.length(), 240));
        }
        markDirty();
    }

    @Override
    public void read(CompoundNBT root) {
        players.clear();
        CompoundNBT all = root.getCompound("Players");
        for (String key : all.keySet()) {
            try {
                UUID playerId = UUID.fromString(key);
                players.put(playerId,
                        PlayerMemory.fromNbt(all.getCompound(key)));
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT root) {
        CompoundNBT all = new CompoundNBT();
        for (Map.Entry<UUID, PlayerMemory> entry : players.entrySet()) {
            all.put(entry.getKey().toString(), entry.getValue().toNbt());
        }
        root.put("Players", all);
        return root;
    }

    public static final class PlayerMemory {
        public int sessions;
        public int sessionsWherePlayerAttacked;
        public int totalHits;
        public int playerWins;
        public int playerLosses;
        public int escapes;
        public long lastSeenGameTime;
        public String lastResult = "NONE";
        public String lastPlayerMessage = "";

        public CompoundNBT toNbt() {
            CompoundNBT nbt = new CompoundNBT();
            nbt.putInt("Sessions", sessions);
            nbt.putInt("AttackSessions", sessionsWherePlayerAttacked);
            nbt.putInt("TotalHits", totalHits);
            nbt.putInt("PlayerWins", playerWins);
            nbt.putInt("PlayerLosses", playerLosses);
            nbt.putInt("Escapes", escapes);
            nbt.putLong("LastSeen", lastSeenGameTime);
            nbt.putString("LastResult", lastResult);
            nbt.putString("LastMessage", lastPlayerMessage);
            return nbt;
        }

        public static PlayerMemory fromNbt(CompoundNBT nbt) {
            PlayerMemory memory = new PlayerMemory();
            memory.sessions = nbt.getInt("Sessions");
            memory.sessionsWherePlayerAttacked = nbt.getInt("AttackSessions");
            memory.totalHits = nbt.getInt("TotalHits");
            memory.playerWins = nbt.getInt("PlayerWins");
            memory.playerLosses = nbt.getInt("PlayerLosses");
            memory.escapes = nbt.getInt("Escapes");
            memory.lastSeenGameTime = nbt.getLong("LastSeen");
            memory.lastResult = nbt.getString("LastResult");
            memory.lastPlayerMessage = nbt.getString("LastMessage");
            return memory;
        }
    }
}
