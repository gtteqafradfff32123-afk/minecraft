package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.*;

public class ZeusStormManager {
    private static final Map<UUID, StormInstance> storms = new HashMap<>();

    private static class StormInstance {
        final ServerPlayerEntity owner;
        int ticks;
        int strikes;

        StormInstance(ServerPlayerEntity owner) {
            this.owner = owner;
            this.ticks = 0;
            this.strikes = 0;
        }
    }

    public static void begin(ServerPlayerEntity owner) {
        storms.put(owner.getUniqueID(), new StormInstance(owner));
    }

    public static void tick(ServerWorld world) {
        Iterator<Map.Entry<UUID, StormInstance>> it = storms.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, StormInstance> entry = it.next();
            StormInstance inst = entry.getValue();
            if (++inst.ticks > 200 || inst.strikes >= 60) {
                it.remove();
                continue;
            }
            if (inst.ticks % 3 == 0) {
                BlockPos center = inst.owner.getPosition();
                BlockPos strikePos = center.add(
                        world.rand.nextInt(24) - 12, 0, world.rand.nextInt(24) - 12);
                net.minecraft.entity.effect.LightningBoltEntity bolt = new net.minecraft.entity.effect.LightningBoltEntity(
                        net.minecraft.entity.EntityType.LIGHTNING_BOLT, world);
                bolt.setPosition(strikePos.getX(), strikePos.getY(), strikePos.getZ());
                world.addEntity(bolt);
                inst.strikes++;

                AxisAlignedBB aoe = new AxisAlignedBB(strikePos).grow(3);
                for (LivingEntity e : world.getEntitiesWithinAABB(LivingEntity.class, aoe,
                        e -> e != inst.owner && e.isAlive())) {
                    e.attackEntityFrom(ModDamageSources.ZEUS_STORM, 3.0F);
                    e.setFire(3);
                    e.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 80, 0));
                    e.addPotionEffect(new EffectInstance(Effects.NAUSEA, 80, 0));
                }
            }
        }
    }
}
