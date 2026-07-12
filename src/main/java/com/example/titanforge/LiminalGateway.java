package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;

/**
 * Thin, validated entry point into the Liminal dimension.
 * Kept separate from {@link com.example.titanforge.liminal.LiminalManager} so callers in the
 * root package can trigger dimension entry without the two same-named classes colliding.
 */
public final class LiminalGateway {
    private LiminalGateway() {}

    public static boolean enter(LivingEntity victim, PlayerEntity owner) {
        if (victim == null || victim.world.isRemote) return false;
        if (!(victim.world instanceof ServerWorld)) return false;
        if (owner != null && !(owner instanceof ServerPlayerEntity)) return false;
        com.example.titanforge.liminal.LiminalManager.enter(
                victim, owner == null ? null : (ServerPlayerEntity) owner);
        return true;
    }

    public static boolean isInside(PlayerEntity player) {
        return com.example.titanforge.liminal.LiminalManager.isInside(player);
    }
}
