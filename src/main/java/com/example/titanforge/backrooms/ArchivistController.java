package com.example.titanforge.backrooms;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.EvokerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.server.ServerWorld;

public final class ArchivistController {
    private ArchivistController() {}

    public static void tick(ServerWorld w, ServerPlayerEntity p, BackroomsSession s) {
        if (!s.bossStarted || s.finished) return;
        if (!p.getPersistentData().getBoolean("TF_ArchivistSpawned")) {
            EvokerEntity boss = EntityType.EVOKER.create(w);
            if (boss == null) return;
            boss.setCustomName(new net.minecraft.util.text.StringTextComponent("§6Архивариус"));
            boss.setCustomNameVisible(true);
            boss.setHealth(boss.getMaxHealth());
            boss.getPersistentData().putBoolean("TF_Archivist", true);
            boss.setPosition(p.getPosX() + 10, p.getPosY(), p.getPosZ() + 10);
            w.addEntity(boss);
            p.getPersistentData().putBoolean("TF_ArchivistSpawned", true);
        }
        if (s.ticks % 200 == 0) p.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 40, 0, false, false));
    }

    public static boolean isArchivist(net.minecraft.entity.Entity e) {
        return e.getPersistentData().getBoolean("TF_Archivist");
    }
}
