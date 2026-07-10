package com.example.titanforge.liminal;

import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class LiminalProtectionHandler {
    @SubscribeEvent
    public void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getWorld() instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) event.getWorld();
        if (world.getDimensionKey() != LiminalDimension.LIMINAL_WORLD) return;
        event.getAffectedBlocks().removeIf(pos -> LiminalManager.isProtectedWall(world, pos));
    }
}
