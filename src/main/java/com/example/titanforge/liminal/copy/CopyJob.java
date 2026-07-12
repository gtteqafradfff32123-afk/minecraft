package com.example.titanforge.liminal.copy;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

public final class CopyJob {
    public enum Phase {
        LOAD_SOURCE,
        LOAD_TARGET,
        CLEAR_TARGET,
        COPY,
        RELEASE,
        DONE,
        FAILED
    }

    public final UUID victim;
    public final ServerWorld source;
    public final ServerWorld target;
    public final BlockPos sourceCenter;
    public final BlockPos targetCenter;
    public final int radius;

    private final List<ChunkPos> sourceChunks = new ArrayList<>();
    private final List<ChunkPos> targetChunks = new ArrayList<>();
    private final Deque<ChunkPos> queue = new ArrayDeque<>();

    private Phase phase = Phase.LOAD_SOURCE;
    private int phaseDone;
    private int blocksCopied;
    private Throwable failure;

    public CopyJob(UUID victim,
                   ServerWorld source,
                   ServerWorld target,
                   BlockPos sourceCenter,
                   BlockPos targetCenter,
                   int radius) {
        this.victim = victim;
        this.source = source;
        this.target = target;
        this.sourceCenter = sourceCenter.toImmutable();
        this.targetCenter = targetCenter.toImmutable();
        this.radius = radius;

        int chunkRadius = (radius >> 4) + 1;
        ChunkPos sourceOrigin = new ChunkPos(sourceCenter);
        ChunkPos targetOrigin = new ChunkPos(targetCenter);

        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                sourceChunks.add(new ChunkPos(
                    sourceOrigin.x + dx,
                    sourceOrigin.z + dz));
                targetChunks.add(new ChunkPos(
                    targetOrigin.x + dx,
                    targetOrigin.z + dz));
            }
        }

        resetQueue(sourceChunks);
    }

    public Phase getPhase() {
        return phase;
    }

    public boolean isDone() {
        return phase == Phase.DONE;
    }

    public boolean isFailed() {
        return phase == Phase.FAILED;
    }

    public Throwable getFailure() {
        return failure;
    }

    public int blocksCopied() {
        return blocksCopied;
    }

    public float progress() {
        if (phase == Phase.DONE) return 1.0F;
        if (phase == Phase.FAILED) return 0.0F;

        int total = phase == Phase.LOAD_SOURCE || phase == Phase.COPY
            ? sourceChunks.size()
            : targetChunks.size();
        float local = total == 0 ? 1.0F : (float) phaseDone / total;

        switch (phase) {
            case LOAD_SOURCE: return local * 0.15F;
            case LOAD_TARGET: return 0.15F + local * 0.10F;
            case CLEAR_TARGET: return 0.25F + local * 0.25F;
            case COPY: return 0.50F + local * 0.48F;
            case RELEASE: return 0.98F;
            default: return 0.0F;
        }
    }

    public void step(int chunksPerTick) {
        if (phase == Phase.DONE || phase == Phase.FAILED) return;

        try {
            int budget = Math.max(1, chunksPerTick);
            while (budget-- > 0
                && phase != Phase.DONE
                && phase != Phase.FAILED) {

                if (phase == Phase.RELEASE) {
                    releaseTickets();
                    phase = Phase.DONE;
                    return;
                }

                ChunkPos pos = queue.pollFirst();
                if (pos == null) {
                    advancePhase();
                    continue;
                }

                switch (phase) {
                    case LOAD_SOURCE:
                        source.getChunk(pos.x, pos.z);
                        source.getChunkProvider().forceChunk(pos, true);
                        break;

                    case LOAD_TARGET:
                        target.getChunk(pos.x, pos.z);
                        target.getChunkProvider().forceChunk(pos, true);
                        break;

                    case CLEAR_TARGET:
                        DeltaCopier.clearChunk(
                            target, pos, targetCenter, radius);
                        break;

                    case COPY:
                        blocksCopied += DeltaCopier.copyChunkFull(
                            source,
                            target,
                            pos,
                            sourceCenter,
                            targetCenter,
                            radius);
                        break;

                    default:
                        break;
                }

                phaseDone++;
            }
        } catch (Throwable throwable) {
            failure = throwable;
            phase = Phase.FAILED;
            releaseTickets();
        }
    }

    public void cancel() {
        releaseTickets();
        phase = Phase.FAILED;
        failure = new IllegalStateException("Copy job cancelled");
    }

    private void advancePhase() {
        phaseDone = 0;

        switch (phase) {
            case LOAD_SOURCE:
                phase = Phase.LOAD_TARGET;
                resetQueue(targetChunks);
                break;

            case LOAD_TARGET:
                phase = Phase.CLEAR_TARGET;
                resetQueue(targetChunks);
                break;

            case CLEAR_TARGET:
                phase = Phase.COPY;
                resetQueue(sourceChunks);
                break;

            case COPY:
                phase = Phase.RELEASE;
                queue.clear();
                break;

            default:
                break;
        }
    }

    private void resetQueue(List<ChunkPos> chunks) {
        queue.clear();
        queue.addAll(chunks);
    }

    private void releaseTickets() {
        for (ChunkPos pos : sourceChunks) {
            try {
                source.getChunkProvider().forceChunk(pos, false);
            } catch (Throwable ignored) {}
        }

        for (ChunkPos pos : targetChunks) {
            try {
                target.getChunkProvider().forceChunk(pos, false);
            } catch (Throwable ignored) {}
        }
    }
}
