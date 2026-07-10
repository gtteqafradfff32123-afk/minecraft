package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;

public final class LiminalManager {
    private LiminalManager() {}

    public static boolean enter(LivingEntity victim, PlayerEntity owner, int ignoredDurationSec) {
        if (victim == null || victim.world.isRemote) return false;
        if (!(victim.world instanceof ServerWorld)) return false;
        if (owner != null && !(owner instanceof ServerPlayerEntity)) return false;
        com.example.titanforge.liminal.LiminalManager.enter(
                victim, owner == null ? null : (ServerPlayerEntity) owner, 360);
        return true;
    }

    public static boolean isInside(PlayerEntity player) {
        return com.example.titanforge.liminal.LiminalManager.isInside(player);
    }
}
