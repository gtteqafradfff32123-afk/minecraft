package com.example.titanforge.liminal;

import com.example.titanforge.TitanForge;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class LiminalArenaCleaner {
    private static final Map<UUID, Job> ACTIVE = new LinkedHashMap<>();
    private static final int CHUNKS_PER_TICK = 1;

    public static void start(ServerWorld world, UUID owner,
                             BlockPos center, int radius,
                             Runnable onComplete) {
        cancel(owner);
        ACTIVE.put(owner, new Job(world, owner,
                center.toImmutable(), radius, onComplete));
    }

    public static boolean isCleaning(UUID owner) {
        return ACTIVE.containsKey(owner);
    }

    public static void cancel(UUID owner) {
        Job old = ACTIVE.remove(owner);
        if (old != null) old.releaseCurrentChunk();
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;

        Iterator<Map.Entry<UUID, Job>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Job> entry = iterator.next();
            Job job = entry.getValue();
            try {
                job.step(CHUNKS_PER_TICK);
            } catch (Throwable error) {
                TitanForge.LOGGER.error(
                        "[liminal-clean] failed for {}", entry.getKey(), error);
                job.releaseCurrentChunk();
                iterator.remove();
                continue;
            }

            if (job.isDone()) {
                iterator.remove();
                Runnable callback = job.onComplete;
                if (callback != null) {
                    job.world.getServer().execute(callback);
                }
            }
        }
    }

    private static final class Job {
        private final ServerWorld world;
        private final UUID owner;
        private final BlockPos center;
        private final int radius;
        private final int radiusSq;
        private final Deque<ChunkPos> chunks = new ArrayDeque<>();
        private final Runnable onComplete;
        private boolean entitiesRemoved;
        private ChunkPos currentForced;

        private Job(ServerWorld world, UUID owner,
                    BlockPos center, int radius,
                    Runnable onComplete) {
            this.world = world;
            this.owner = owner;
            this.center = center;
            this.radius = radius;
            this.radiusSq = radius * radius;
            this.onComplete = onComplete;

            ChunkPos middle = new ChunkPos(center);
            int chunkRadius = (radius >> 4) + 2;
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    chunks.addLast(new ChunkPos(middle.x + dx, middle.z + dz));
                }
            }
        }

        private boolean isDone() {
            return entitiesRemoved && chunks.isEmpty();
        }

        private void step(int budget) {
            if (!entitiesRemoved) {
                removeEntities();
                entitiesRemoved = true;
            }

            for (int i = 0; i < budget && !chunks.isEmpty(); i++) {
                ChunkPos chunk = chunks.removeFirst();
                clearChunk(chunk);
            }
        }

        private void removeEntities() {
            AxisAlignedBB box = new AxisAlignedBB(
                    center.getX() - radius,
                    0,
                    center.getZ() - radius,
                    center.getX() + radius + 1,
                    256,
                    center.getZ() + radius + 1);

            for (Entity entity : world.getEntitiesWithinAABB(
                    Entity.class, box,
                    entity -> !(entity instanceof PlayerEntity))) {
                entity.remove();
            }
        }

        private void clearChunk(ChunkPos chunk) {
            world.getChunk(chunk.x, chunk.z);
            world.getChunkProvider().forceChunk(chunk, true);
            currentForced = chunk;

            BlockPos.Mutable pos = new BlockPos.Mutable();
            int startX = chunk.getXStart();
            int startZ = chunk.getZStart();

            for (int localX = 0; localX < 16; localX++) {
                int x = startX + localX;
                int dx = x - center.getX();

                for (int localZ = 0; localZ < 16; localZ++) {
                    int z = startZ + localZ;
                    int dz = z - center.getZ();
                    if (dx * dx + dz * dz > radiusSq) continue;

                    for (int y = 0; y < 256; y++) {
                        pos.setPos(x, y, z);
                        TileEntity tile = world.getTileEntity(pos);
                        if (tile != null) world.removeTileEntity(pos);
                        if (!world.isAirBlock(pos)) {
                            world.setBlockState(pos,
                                    Blocks.AIR.getDefaultState(), 18);
                        }
                    }
                }
            }

            releaseCurrentChunk();
        }

        private void releaseCurrentChunk() {
            if (currentForced == null) return;
            world.getChunkProvider().forceChunk(currentForced, false);
            currentForced = null;
        }
    }
}
