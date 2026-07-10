package com.example.titanforge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class ZeusStormManager {
    private static final int DURATION = 600;
    private static final int STRIKE_INTERVAL = 10;
    private static final int RADIUS = 12;
    private static final Map<UUID, StormInstance> STORMS = new HashMap<>();

    private static final class StormInstance {
        final UUID owner;
        final RegistryKey<World> worldKey;
        long startTick;

        StormInstance(ServerPlayerEntity owner) {
            this.owner = owner.getUniqueID();
            this.worldKey = owner.world.getDimensionKey();
            this.startTick = owner.world.getGameTime();
        }
    }

    private ZeusStormManager() {}

    public static void begin(ServerPlayerEntity owner) {
        STORMS.put(owner.getUniqueID(), new StormInstance(owner));
    }

    public static void tick(ServerWorld world) {
        Iterator<Map.Entry<UUID, StormInstance>> it = STORMS.entrySet().iterator();
        while (it.hasNext()) {
            StormInstance storm = it.next().getValue();
            if (storm.worldKey != world.getDimensionKey()) continue;

            ServerPlayerEntity owner = world.getServer().getPlayerList().getPlayerByUUID(storm.owner);
            long age = world.getGameTime() - storm.startTick;
            if (owner == null || !owner.isAlive() || owner.world != world || age >= DURATION) {
                it.remove();
                continue;
            }
            if (age % STRIKE_INTERVAL != 0) continue;

            int x = owner.getPosition().getX() + world.rand.nextInt(RADIUS * 2) - RADIUS;
            int z = owner.getPosition().getZ() + world.rand.nextInt(RADIUS * 2) - RADIUS;
            BlockPos surface = world.getHeight(net.minecraft.world.gen.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                    new BlockPos(x, owner.getPosition().getY(), z));

            LightningBoltEntity bolt = net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(world);
            if (bolt != null) {
                bolt.setPosition(surface.getX() + 0.5D, surface.getY(), surface.getZ() + 0.5D);
                world.addEntity(bolt);
            }

            AxisAlignedBB area = new AxisAlignedBB(surface).grow(3.0D);
            for (LivingEntity target : world.getEntitiesWithinAABB(LivingEntity.class, area,
                    e -> e.isAlive() && e != owner && !ChaosDevourHandler.isOwnThrall(e, owner))) {
                target.hurtResistantTime = 0;
                target.attackEntityFrom(ModDamageSources.ZEUS_STORM, 3.0F);
                target.setFire(3);
                target.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 80, 0));
                target.addPotionEffect(new EffectInstance(Effects.NAUSEA, 80, 0));
            }
        }
    }

    public static void clear(UUID owner) {
        STORMS.remove(owner);
    }
}
