package com.example.titanforge.liminal;

import com.example.titanforge.entities.ShadowEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.server.ServerWorld;

public final class LiminalDirector {
    private LiminalDirector() {}

    public static void tick(ServerWorld world, ServerPlayerEntity player, LiminalManager.State state) {
        state.directorTimer++;
        state.independentCopyTimer++;

        int stage = Math.min(10, state.ticks / 1200 + state.copiesKilled);
        if (stage > state.collapseStage) {
            state.collapseStage = stage;
            state.anomalyIntensity = 1 + stage / 2;
            LiminalManager.sendCopyMessage(player, stageMessage(stage));
            world.spawnParticle(ParticleTypes.REVERSE_PORTAL,
                    player.getPosX(), player.getPosY() + 1.0D, player.getPosZ(),
                    50, 1.2D, 1.0D, 1.2D, 0.1D);
        }

        int copyInterval = Math.max(140, 520 - stage * 32);
        if (state.firstHit && state.independentCopyTimer >= copyInterval) {
            state.independentCopyTimer = 0;
            int count = stage >= 7 ? 3 : stage >= 4 ? 2 : 1;
            for (int i = 0; i < count; i++) {
                LiminalManager.spawnDirectorCopy(world, state, player, stage);
            }
        }

        if (stage >= 3 && state.directorTimer % 300 == 0) {
            int r = world.rand.nextInt(3);
            if (r == 0) {
                LiminalManager.spawnArmedCopy(world, state, player);
            } else if (r == 1) {
                LiminalManager.spawnArmedSkeleton(world, state, player);
            } else {
                LiminalManager.spawnGhostMob(world, state, player);
            }
        }

        if (stage >= 5 && state.directorTimer % 400 == 0) {
            player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 35, 0, false, false));
        }
        if (stage >= 8 && state.directorTimer % 260 == 0) {
            player.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 50, 1, false, false));
        }

        if (!state.shadowSpawned && state.shadowRespawnTimer <= 0 && stage >= 2) {
            state.shadowRespawnTimer = Math.max(80, 260 - stage * 18);
        }

        if (state.shadowPhase.ordinal() >= ShadowPhase.HUNTER.ordinal()
                && state.directorTimer % 500 == 0) {
            LiminalManager.markNearestCopy(world, state, player);
        }
    }

    private static String stageMessage(int stage) {
        if (stage <= 2) return "\u00A78\u041F\u0440\u043E\u0441\u0442\u0440\u0430\u043D\u0441\u0442\u0432\u043E \u0437\u0430\u043C\u0435\u0442\u0438\u043B\u043E \u0442\u0435\u0431\u044F.";
        if (stage <= 4) return "\u00A78\u041A\u043E\u043C\u043D\u0430\u0442\u044B \u0431\u043E\u043B\u044C\u0448\u0435 \u043D\u0435 \u043F\u043E\u043C\u043D\u044F\u0442 \u0441\u0432\u043E\u044E \u0444\u043E\u0440\u043C\u0443.";
        if (stage <= 6) return "\u00A74\u0422\u0432\u043E\u0438 \u043A\u043E\u043F\u0438\u0438 \u0438\u0434\u0443\u0442 \u0431\u0435\u0437 \u043D\u0435\u0451.";
        if (stage <= 8) return "\u00A74\u0421\u0442\u0435\u043D\u044B \u0443\u0447\u0430\u0442\u0441\u044F \u0437\u0430\u043A\u0440\u044B\u0432\u0430\u0442\u044C\u0441\u044F.";
        return "\u00A70\u041C\u0438\u0440 \u0440\u0435\u0448\u0438\u043B \u043E\u0441\u0442\u0430\u0432\u0438\u0442\u044C \u0442\u043E\u043B\u044C\u043A\u043E \u043E\u0434\u043D\u043E\u0433\u043E.";
    }
}
