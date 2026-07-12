package com.example.titanforge.liminal.copy;

import com.example.titanforge.TitanForge;
import com.example.titanforge.liminal.LiminalManager;
import com.example.titanforge.liminal.screen.LimboHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public final class ChunkCopyManager {
    private static final List<CopyJob> ACTIVE = new ArrayList<>();
    private static final int CHUNKS_PER_TICK = 2;

    public static void enqueue(CopyJob job) {
        cancelFor(job.victim);
        ACTIVE.add(job);
    }

    public static void cancelFor(UUID playerId) {
        Iterator<CopyJob> it = ACTIVE.iterator();
        while (it.hasNext()) {
            CopyJob job = it.next();
            if (job.victim.equals(playerId)) {
                job.cancel();
                it.remove();
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;

        Iterator<CopyJob> it = ACTIVE.iterator();
        while (it.hasNext()) {
            CopyJob job = it.next();
            job.step(CHUNKS_PER_TICK);

            ServerPlayerEntity player = job.target.getServer()
                    .getPlayerList().getPlayerByUUID(job.victim);
            if (player != null) LimboHandler.updateProgress(player, job.progress());

            if (job.isFailed()) {
                TitanForge.LOGGER.error("[liminal] copy failed for {}", job.victim,
                        job.getFailure());
                if (player != null) LiminalManager.forceExit(player, true);
                it.remove();
            } else if (job.isDone()) {
                LiminalManager.onCloneReady(job);
                it.remove();
            }
        }
    }
}
