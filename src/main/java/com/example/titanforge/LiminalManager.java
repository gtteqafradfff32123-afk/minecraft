package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.world.server.ServerWorld;

public class LiminalManager {

    public static void enter(LivingEntity victim, PlayerEntity owner, int durationSec) {
        com.example.titanforge.liminal.LiminalManager.enter(victim, (ServerPlayerEntity) owner, durationSec);
    }

    public static boolean isInside(PlayerEntity p) {
        return com.example.titanforge.liminal.LiminalManager.isInside(p);
    }
}
