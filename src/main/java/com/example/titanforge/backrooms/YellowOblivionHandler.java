package com.example.titanforge.backrooms;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;

public final class YellowOblivionHandler {
    private YellowOblivionHandler() {}

    public static boolean handleTeleport(ServerPlayerEntity player) {
        if (!BackroomsDimension.WORLD.equals(player.world.getDimensionKey())) return false;
        BackroomsSessionManager.tick((ServerWorld) player.world);
        return true;
    }
}
