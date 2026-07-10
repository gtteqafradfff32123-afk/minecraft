package com.example.titanforge.liminal.copy;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public class CopyJob {
    public final UUID victim;
    public final ServerWorld source;
    public final ServerWorld target;
    public final BlockPos center;
    private final Deque<ChunkPos> queue = new ArrayDeque<>();
    private final int totalChunks;
    private int done = 0;
    private int blocksCopied = 0;

    public CopyJob(UUID victim, ServerWorld source, ServerWorld target, BlockPos center, int radiusBlocks) {
        this.victim = victim;
        this.source = source;
        this.target = target;
        this.center = center;

        int cr = (radiusBlocks >> 4) + 1;
        ChunkPos c = new ChunkPos(center);
        for (int dx = -cr; dx <= cr; dx++)
            for (int dz = -cr; dz <= cr; dz++)
                queue.add(new ChunkPos(c.x + dx, c.z + dz));
        this.totalChunks = queue.size();
    }

    public boolean isDone() { return queue.isEmpty(); }
    public float progress() { return totalChunks == 0 ? 1f : (float) done / totalChunks; }
    public int blocksCopied() { return blocksCopied; }

    public void step(int chunksPerTick) {
        for (int i = 0; i < chunksPerTick && !queue.isEmpty(); i++) {
            ChunkPos pos = queue.poll();
            if (!source.getChunkProvider().isChunkLoaded(pos)) {
                done++;
                continue;
            }
            blocksCopied += DeltaCopier.copyChunkFull(source, target, pos, center, 100);
            done++;
        }
    }
}
