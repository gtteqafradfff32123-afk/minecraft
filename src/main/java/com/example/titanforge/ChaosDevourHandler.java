package com.example.titanforge;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID)
public class ChaosDevourHandler {

    private static final Map<UUID, Integer> hitCounter = new HashMap<>();
    private static final Map<UUID, Long> vortexCooldown = new HashMap<>();
    private static final Map<UUID, UUID> activeThralls = new HashMap<>();
    private static final Map<UUID, UUID> thrallOwners = new HashMap<>();
    private static final List<VortexInstance> activeVortexes = new ArrayList<>();
    private static final Set<UUID> processingCleave = new HashSet<>();

    private static final Field GOAL_SELECTOR_FIELD = getField(MobEntity.class, "goalSelector", "field_70714_bg");
    private static final Field TARGET_SELECTOR_FIELD = getField(MobEntity.class, "targetSelector", "field_70715_bh");
    private static final Field GOALS_FIELD = getField(GoalSelector.class, "goals", "field_220898_b");
    private static final Field FLAG_GOALS_FIELD = getField(GoalSelector.class, "flagGoals", "field_220897_a");

    private static final long VORTEX_COOLDOWN_TICKS = 160;
    private static final int VORTEX_RADIUS = 3;
    private static final int VORTEX_DURATION_TICKS = 30;
    private static final double VORTEX_PULL_STRENGTH = 0.4;
    private static final double THRALL_CHANCE = 0.15;
    private static final double ABSORPTION_CHANCE = 0.4;
    private static final int THRALL_LIFETIME_TICKS = 600;
    private static final int THRALL_HP = 20;
    private static final double THRALL_TELEPORT_DIST = 10.0;
    private static final int HITS_FOR_VORTEX = 4;
    static final String SOUL_COPY = SoulReaperHandler.SOUL_COPY;

    private static class VortexInstance {
        final ServerWorld world;
        final Vector3d position;
        final UUID ownerId;
        int ticksLeft;

        VortexInstance(ServerWorld world, Vector3d pos, UUID owner, int duration) {
            this.world = world;
            this.position = pos;
            this.ownerId = owner;
            this.ticksLeft = duration;
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getPlayer().getUniqueID();
        hitCounter.remove(id);
        vortexCooldown.remove(id);
        activeThralls.remove(id);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        try {
            Entity sourceEntity = event.getSource().getTrueSource();
            LivingEntity target = event.getEntityLiving();
            World world = target.world;
            if (world.isRemote) return;

            if (target instanceof PlayerEntity && sourceEntity instanceof LivingEntity
                    && !(sourceEntity instanceof PlayerEntity) && !isThrall((LivingEntity) sourceEntity)) {
                ServerWorld sw = (ServerWorld) world;
                for (Map.Entry<UUID, UUID> entry : new HashMap<>(thrallOwners).entrySet()) {
                    if (entry.getValue().equals(target.getUniqueID())) {
                        Entity thrall = sw.getEntityByUuid(entry.getKey());
                        if (thrall instanceof MobEntity && thrall.isAlive()) {
                            ((MobEntity) thrall).setAttackTarget((LivingEntity) sourceEntity);
                        }
                    }
                }
            }

            if (sourceEntity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) sourceEntity;

                if (isOwnThrall(target, player)) {
                    if (target instanceof MobEntity) {
                        ((MobEntity) target).setAttackTarget(null);
                    }
                    event.setCanceled(true);
                    return;
                }

                if (isThrall(target)) {
                    event.setCanceled(true);
                    return;
                }

                ItemStack weapon = player.getHeldItemMainhand();
                int level = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CHAOS_DEVOUR.get(), weapon);
                if (level <= 0) return;

                float healAmount = event.getAmount() * 0.05F * level;
                player.heal(healAmount);

                UUID playerId = player.getUniqueID();

                if (level >= 3 && !processingCleave.contains(playerId)) {
                    processingCleave.add(playerId);
                    try {
                        float cleaveDmg = event.getAmount() * 0.5F;
                        for (LivingEntity e : world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(target.getPosition()).grow(3.0))) {
                            if (e != target && e != player && !isOwnThrall(e, player)) {
                                e.hurtResistantTime = 0;
                                e.attackEntityFrom(DamageSource.causePlayerDamage(player), cleaveDmg);
                                ((ServerWorld) world).spawnParticle(ParticleTypes.DAMAGE_INDICATOR, e.getPosX(), e.getPosY() + 1.0, e.getPosZ(), 10, 0.3, 0.5, 0.3, 0.05);
                            }
                        }
                        ((ServerWorld) world).spawnParticle(ParticleTypes.LARGE_SMOKE, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 30, 1.5, 0.5, 1.5, 0.05);
                    } finally {
                        processingCleave.remove(playerId);
                    }
                }

                int hits = hitCounter.getOrDefault(playerId, 0) + 1;
                hitCounter.put(playerId, hits);

                if (hits >= HITS_FOR_VORTEX) {
                    hitCounter.put(playerId, 0);
                    long gameTime = world.getGameTime();
                    long lastVortex = vortexCooldown.getOrDefault(playerId, 0L);

                    if (gameTime - lastVortex >= VORTEX_COOLDOWN_TICKS) {
                        vortexCooldown.put(playerId, gameTime);
                        activeVortexes.add(new VortexInstance((ServerWorld) world, target.getPositionVec(), playerId, VORTEX_DURATION_TICKS));
                    }
                }
            }

            if (isThrall(target)) {
                Entity trueSrc = event.getSource().getTrueSource();
                if (trueSrc instanceof LivingEntity && !(trueSrc instanceof PlayerEntity)) {
                    LivingEntity attacker = (LivingEntity) trueSrc;
                    UUID ownerId = thrallOwners.get(target.getUniqueID());
                    if (ownerId != null && !isThrall(attacker)) {
                        if (target instanceof MobEntity) {
                            ((MobEntity) target).setAttackTarget(attacker);
                        }
                        if (event.getAmount() > 0) {
                            attacker.attackEntityFrom(DamageSource.causeMobDamage((LivingEntity) target), 3.0F);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            System.out.println("[TitanForge-ChaosDevour-onLivingHurt] " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        try {
            LivingEntity dead = event.getEntityLiving();
            World world = dead.world;
            if (world.isRemote) return;

            if (isThrall(dead) && thrallOwners.containsKey(dead.getUniqueID())) {
                spawnSoulExplosion((ServerWorld) world, dead.getPositionVec());
                UUID ownerId = thrallOwners.remove(dead.getUniqueID());
                activeThralls.remove(ownerId);
                return;
            }

            if (!(event.getSource().getTrueSource() instanceof PlayerEntity)) return;
            PlayerEntity player = (PlayerEntity) event.getSource().getTrueSource();

            ItemStack weapon = player.getHeldItemMainhand();
            int level = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CHAOS_DEVOUR.get(), weapon);
            if (level <= 0) return;

            if (level >= 2 && !dead.isEntityUndead()) {
                if (world.rand.nextDouble() < ABSORPTION_CHANCE) {
                    player.addPotionEffect(new EffectInstance(Effects.ABSORPTION, 300, 0, false, true));
                    player.addPotionEffect(new EffectInstance(Effects.SPEED, 60, 0, false, true));
                    ServerWorld sw = (ServerWorld) world;
                    sw.spawnParticle(ParticleTypes.SOUL,
                            player.getPosX(), player.getPosY() + 1.0, player.getPosZ(),
                            15, 0.5, 0.5, 0.5, 0.02);
                }
            }

            if (level >= 3 && dead.getMaxHealth() <= 40 && !(dead instanceof PlayerEntity)) {
                if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SOUL_REAPER.get(), weapon) > 0) return;
                if (world.rand.nextDouble() < THRALL_CHANCE) {
                    spawnThrall((ServerWorld) world, player, dead);
                }
            }
        } catch (Throwable t) {
            System.out.println("[TitanForge-ChaosDevour-onLivingDeath] " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        try {
            if (event.phase != TickEvent.Phase.END) return;
            if (!(event.world instanceof ServerWorld)) return;
            ServerWorld world = (ServerWorld) event.world;

            for (Entity e : world.getEntities().collect(java.util.stream.Collectors.toList())) {
                if (e instanceof MobEntity && e.getPersistentData().getBoolean("ChaosDevourThrall")) {
                    if (e.getPersistentData().contains("ThrallExpireTime") && world.getGameTime() > e.getPersistentData().getLong("ThrallExpireTime")) {
                        e.remove();
                        thrallOwners.remove(e.getUniqueID());
                        continue;
                    }
                    if (!e.getPersistentData().hasUniqueId("ThrallOwner")) {
                        continue;
                    }
                    UUID owner = e.getPersistentData().getUniqueId("ThrallOwner");
                    if (!thrallOwners.containsKey(e.getUniqueID())) {
                        thrallOwners.put(e.getUniqueID(), owner);
                    }
                }
            }

            Iterator<VortexInstance> vortexIt = activeVortexes.iterator();
            while (vortexIt.hasNext()) {
                VortexInstance vortex = vortexIt.next();
                if (vortex.world != world) continue;

                vortex.ticksLeft--;
                if (vortex.ticksLeft <= 0) {
                    vortexIt.remove();
                    continue;
                }

                AxisAlignedBB area = new AxisAlignedBB(
                        vortex.position.x - VORTEX_RADIUS, vortex.position.y - VORTEX_RADIUS, vortex.position.z - VORTEX_RADIUS,
                        vortex.position.x + VORTEX_RADIUS, vortex.position.y + VORTEX_RADIUS, vortex.position.z + VORTEX_RADIUS
                );

                List<LivingEntity> entities = world.getEntitiesWithinAABB(LivingEntity.class, area,
                        e -> !(e instanceof PlayerEntity) && e.isAlive());

                for (LivingEntity entity : entities) {
                    Vector3d pull = vortex.position.subtract(entity.getPositionVec()).normalize().scale(VORTEX_PULL_STRENGTH);
                    entity.setMotion(entity.getMotion().add(pull));
                    entity.velocityChanged = true;
                }

                for (int i = 0; i < 8; i++) {
                    double angle = world.rand.nextDouble() * Math.PI * 2;
                    double r = world.rand.nextDouble() * VORTEX_RADIUS;
                    world.spawnParticle(ParticleTypes.PORTAL,
                            vortex.position.x + Math.cos(angle) * r,
                            vortex.position.y + 0.5,
                            vortex.position.z + Math.sin(angle) * r,
                            2, 0, 0.1, 0, 0.05);
                    world.spawnParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            vortex.position.x + Math.cos(angle) * (r * 0.5),
                            vortex.position.y + 0.3,
                            vortex.position.z + Math.sin(angle) * (r * 0.5),
                            1, 0, 0.05, 0, 0.01);
                }
            }

            for (Map.Entry<UUID, UUID> entry : new HashMap<>(thrallOwners).entrySet()) {
                Entity thrall = world.getEntityByUuid(entry.getKey());
                if (thrall == null || !thrall.isAlive()) {
                    thrallOwners.remove(entry.getKey());
                    continue;
                }
                if (world.getGameTime() % 5 == 0) {
                    world.spawnParticle(ParticleTypes.SOUL,
                            thrall.getPosX(), thrall.getPosY() + 1.5, thrall.getPosZ(),
                            2, 0.3, 0.3, 0.3, 0.01);
                    world.spawnParticle(ParticleTypes.REVERSE_PORTAL,
                            thrall.getPosX(), thrall.getPosY() + 0.5, thrall.getPosZ(),
                            3, 0.2, 0.4, 0.2, 0.02);
                }
            }
        } catch (Throwable t) {
            System.out.println("[TitanForge-ChaosDevour-onWorldTick] " + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace();
        }
    }

    private static void spawnThrall(ServerWorld world, PlayerEntity owner, LivingEntity victim) {
        UUID playerId = owner.getUniqueID();

        Entity raw = victim.getType().create(world);
        if (!(raw instanceof MobEntity)) return;
        MobEntity thrall = (MobEntity) raw;
        thrall.setPosition(victim.getPosX(), victim.getPosY(), victim.getPosZ());
        thrall.setHealth(THRALL_HP);
        thrall.setCustomName(new StringTextComponent("\u00A75\u00A7l\u2620 Risen Thrall"));
        thrall.setCustomNameVisible(true);
        thrall.enablePersistence();
        thrall.setCanPickUpLoot(false);
        thrall.getPersistentData().putBoolean("ChaosDevourThrall", true);
        thrall.getPersistentData().putUniqueId("ThrallOwner", playerId);
        thrall.getPersistentData().putLong("ThrallExpireTime", world.getGameTime() + THRALL_LIFETIME_TICKS);

        clearSelector(getGoalSelector(thrall));
        clearSelector(getTargetSelector(thrall));

        getGoalSelector(thrall).addGoal(1, new FollowOwnerGoal(thrall, owner, THRALL_TELEPORT_DIST, 3.0, 1.0));
        getGoalSelector(thrall).addGoal(2, new LookAtGoal(thrall, PlayerEntity.class, 8.0f));
        getGoalSelector(thrall).addGoal(3, new LookRandomlyGoal(thrall));

        getTargetSelector(thrall).addGoal(1, new NearestAttackableTargetGoal<>(thrall, LivingEntity.class, 10, true, false,
            target -> {
                if (target == owner || !target.isAlive()) return false;
                if (target.getPersistentData().getBoolean("ChaosDevourThrall")) return false;
                if (target.getPersistentData().getBoolean("IsSoulCopy")) return false;
                if (target instanceof MonsterEntity) return true;
                if (target instanceof MobEntity) {
                    return ((MobEntity) target).getAttackTarget() == owner
                        || target.getRevengeTarget() == owner;
                }
                return false;
            }));

        thrall.addPotionEffect(new EffectInstance(Effects.GLOWING, THRALL_LIFETIME_TICKS, 0, false, true));
        thrall.addPotionEffect(new EffectInstance(Effects.FIRE_RESISTANCE, THRALL_LIFETIME_TICKS, 0, false, false));

        world.addEntity(thrall);

        UUID thrallUuid = thrall.getUniqueID();
        thrallOwners.put(thrallUuid, playerId);

        world.spawnParticle(ParticleTypes.SOUL_FIRE_FLAME,
                victim.getPosX(), victim.getPosY() + 0.5, victim.getPosZ(),
                30, 0.5, 0.8, 0.5, 0.05);
        world.spawnParticle(ParticleTypes.LARGE_SMOKE,
                victim.getPosX(), victim.getPosY(), victim.getPosZ(),
                20, 0.3, 0.5, 0.3, 0.02);
    }

    private static GoalSelector getGoalSelector(MobEntity mob) {
        try {
            if (GOAL_SELECTOR_FIELD != null) return (GoalSelector) GOAL_SELECTOR_FIELD.get(mob);
        } catch (Exception ignored) {
        }
        return new GoalSelector(null);
    }

    private static GoalSelector getTargetSelector(MobEntity mob) {
        try {
            if (TARGET_SELECTOR_FIELD != null) return (GoalSelector) TARGET_SELECTOR_FIELD.get(mob);
        } catch (Exception ignored) {
        }
        return new GoalSelector(null);
    }

    private static Field getField(Class<?> clazz, String... names) {
        for (String name : names) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException ignored) {
            }
        }
        return null;
    }

    private static void clearSelector(GoalSelector selector) {
        try {
            if (GOALS_FIELD != null) {
                Set<?> goals = (Set<?>) GOALS_FIELD.get(selector);
                if (goals != null) goals.clear();
            }
            if (FLAG_GOALS_FIELD != null) {
                Map<?, ?> flags = (Map<?, ?>) FLAG_GOALS_FIELD.get(selector);
                if (flags != null) flags.clear();
            }
        } catch (Exception ignored) {
        }
    }

    private static void spawnSoulExplosion(ServerWorld world, Vector3d pos) {
        world.spawnParticle(ParticleTypes.SOUL,
                pos.x, pos.y + 1.0, pos.z, 40, 0.8, 0.8, 0.8, 0.1);
        world.spawnParticle(ParticleTypes.EXPLOSION,
                pos.x, pos.y + 0.5, pos.z, 3, 0.5, 0.5, 0.5, 0.0);
        world.spawnParticle(ParticleTypes.REVERSE_PORTAL,
                pos.x, pos.y + 1.0, pos.z, 50, 0.5, 1.0, 0.5, 0.15);
    }

    public static boolean isOwnThrall(LivingEntity entity, PlayerEntity player) {
        UUID ownerId = thrallOwners.get(entity.getUniqueID());
        if (ownerId != null && ownerId.equals(player.getUniqueID())) return true;
        if (!entity.getPersistentData().hasUniqueId("ThrallOwner")) return false;
        UUID storedOwner = entity.getPersistentData().getUniqueId("ThrallOwner");
        return storedOwner.equals(player.getUniqueID());
    }

    public static boolean isThrall(LivingEntity entity) {
        boolean mapOk = thrallOwners.containsKey(entity.getUniqueID());
        boolean nbtOk = entity.getPersistentData().getBoolean("ChaosDevourThrall");
        return mapOk || nbtOk;
    }
}
