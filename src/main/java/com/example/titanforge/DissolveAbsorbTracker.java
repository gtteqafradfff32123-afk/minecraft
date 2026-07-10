package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;

import java.util.*;

public class DissolveAbsorbTracker {
    private static final Map<UUID, AbsorbInstance> tracked = new HashMap<>();

    private static class AbsorbInstance {
        final LivingEntity victim;
        final PlayerEntity owner;
        int duration;
        int bonusDamage;

        AbsorbInstance(LivingEntity victim, PlayerEntity owner, int duration) {
            this.victim = victim;
            this.owner = owner;
            this.duration = duration;
            this.bonusDamage = 2;
        }
    }

    public static void mark(LivingEntity victim, PlayerEntity owner, int duration) {
        tracked.put(victim.getUniqueID(), new AbsorbInstance(victim, owner, duration));
    }

    public static void onDeath(LivingEntity dead) {
        AbsorbInstance inst = tracked.remove(dead.getUniqueID());
        if (inst == null) return;
        CompoundNBT data = inst.owner.getPersistentData();
        int dmg = data.getInt("DissolveAbsorbBonus");
        dmg = Math.min(dmg + 2, 10);
        data.putInt("DissolveAbsorbBonus", dmg);
        data.putLong("DissolveAbsorbExpiry", dead.world.getGameTime() + 1200);
    }

    public static int getBonus(PlayerEntity player) {
        CompoundNBT data = player.getPersistentData();
        long expiry = data.getLong("DissolveAbsorbExpiry");
        if (player.world.getGameTime() > expiry) {
            data.remove("DissolveAbsorbBonus");
            data.remove("DissolveAbsorbExpiry");
            return 0;
        }
        return data.getInt("DissolveAbsorbBonus");
    }

    public static void tick() {
        tracked.entrySet().removeIf(e -> !e.getValue().victim.isAlive());
    }
}
