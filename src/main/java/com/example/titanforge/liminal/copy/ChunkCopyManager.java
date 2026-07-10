package com.example.titanforge.liminal.copy;

import com.example.titanforge.liminal.LiminalManager;
import com.example.titanforge.liminal.screen.LimboHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import java.util.*;

public class ChunkCopyManager {
    private static final List<CopyJob> ACTIVE = new ArrayList<>();
    private static final int CHUNKS_PER_TICK = 16;

    public static void enqueue(CopyJob job) {
        ACTIVE.add(job);
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ACTIVE.isEmpty()) return;

        Iterator<CopyJob> it = ACTIVE.iterator();
        while (it.hasNext()) {
            CopyJob job = it.next();
            job.step(CHUNKS_PER_TICK);

            ServerPlayerEntity p = job.target.getServer().getPlayerList().getPlayerByUUID(job.victim);
            if (p != null) LimboHandler.updateProgress(p, job.progress());

            if (job.isDone()) {
                LiminalManager.onCloneReady(job);
                it.remove();
            }
        }
    }
}
