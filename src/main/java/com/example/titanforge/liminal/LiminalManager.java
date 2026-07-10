package com.example.titanforge.liminal;

import com.google.common.collect.ImmutableMap;
import com.example.titanforge.TitanForge;
import com.example.titanforge.entities.PlayerCopyEntity;
import com.example.titanforge.entities.ShadowEntity;
import com.example.titanforge.entities.StunZombieEntity;
import com.example.titanforge.liminal.ai.LiminalDialogue;
import com.example.titanforge.liminal.chat.LiminalChatAI;
import com.example.titanforge.liminal.copy.ChunkCopyManager;
import com.example.titanforge.liminal.copy.CopyJob;
import com.example.titanforge.liminal.copy.DeltaCopier;
import com.example.titanforge.liminal.screen.LimboHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.BlockTags;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;

import java.util.*;

public class LiminalManager {
    private static final int RADIUS = 100;
    private static final int DURATION_TICKS = 7200;
    private static final int KILLS_TO_ESCAPE = 6;
    private static final int UNFREEZE_TICKS = 60;
    public static final int SHADOW_BEHAVIOR_STALK = 0;
    public static final int SHADOW_BEHAVIOR_WANDER = 1;
    public static final int SHADOW_BEHAVIOR_STAY = 2;

    private static final Map<Block, Block> ROT_CHAIN = ImmutableMap.of(
        Blocks.GRASS_BLOCK, Blocks.DIRT,
        Blocks.DIRT,        Blocks.COARSE_DIRT,
        Blocks.COARSE_DIRT, Blocks.SOUL_SAND
    );

    public static class State {
        public UUID victim;
        public BlockPos center;
        public int ticks = 0;
        public int durationTicks;
        public int copiesKilled = 0;
        public int activeCopies = 0;
        public boolean cloneReady = false;
        public boolean markDeadOnRejoin = false;
        public boolean shadowSpawned = false;
        public boolean frozen = false;
        public boolean secondHalfAnomalies = false;
        public boolean shadowAggressive = false;
        public boolean shadowAwakened = false;
        public boolean firstHit = false;
        public boolean musicStarted = false;
        public int shadowBehavior = SHADOW_BEHAVIOR_STALK;
        public int shadowAnger = 0;
        public int shadowRespawnTimer = 0;
        public BlockPos realReturnPos;
        public BlockPos shadowHoldPos;
        public int ambientTimer = 0;
        public UUID shadowId;
        public List<UUID> copyIds = new ArrayList<>();
        public Map<UUID, Integer> spawnTicks = new HashMap<>();
        public int directorTimer = 0;
        public int independentCopyTimer = 0;
        public int anomalyIntensity = 1;
        public int collapseStage = 0;
        public ShadowPhase shadowPhase = ShadowPhase.FRIENDLY;
        public int shadowLivesBroken = 0;
        public int shadowAbilityCooldown = 0;
        public int shadowHuntPressure = 0;
        public int shadowFinalTimer = 0;
        public UUID markedCopyId;
        public BlockPos lastSafePlayerPos;
        public final List<BlockPos> finalAnchors = new ArrayList<>();
        public boolean finalRitualStarted = false;
        public int arrowsUsed;
        public int shieldBlocks;
        public int sprintTicks;
        public int timeLookingAtShadow;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static void applyLiminalHit(LivingEntity target) {
        target.addPotionEffect(new EffectInstance(Effects.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        target.addPotionEffect(new EffectInstance(Effects.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
        target.addPotionEffect(new EffectInstance(Effects.JUMP_BOOST, Integer.MAX_VALUE, 128, false, false));
    }

    public static void enter(LivingEntity victim, ServerPlayerEntity owner, int durationSec) {
        if (!(victim instanceof ServerPlayerEntity)) {
            enterMob(victim, (ServerWorld) victim.world);
            return;
        }
        ServerPlayerEntity vp = (ServerPlayerEntity) victim;
        ServerWorld overworld = (ServerWorld) vp.world;
        ServerWorld clone = LiminalDimension.get(vp.getServer());
        if (clone == null) {
            vp.sendStatusMessage(new StringTextComponent("\u00A7c\u041B\u0438\u043C\u0438\u043D\u0430\u043B\u044C\u043D\u043E\u0435 \u0438\u0437\u043C\u0435\u0440\u0435\u043D\u0438\u0435 \u043D\u0435 \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u043E (\u043D\u0443\u0436\u0435\u043D \u043D\u043E\u0432\u044B\u0439 \u043C\u0438\u0440)"), true);
            return;
        }

        State st = new State();
        st.victim = vp.getUniqueID();
        st.center = vp.getPosition();
        st.realReturnPos = vp.getPosition();
        st.durationTicks = DURATION_TICKS;
        st.frozen = true;
        STATES.put(st.victim, st);

        TitanForge.LOGGER.info("[liminal] enter: player={} center={} realReturn={}",
            vp.getName().getString(), st.center, st.realReturnPos);

        applyLiminalHit(vp);

        LimboHandler.enterLimbo(vp);
        CopyJob job = new CopyJob(st.victim, overworld, clone, st.center, RADIUS);
        ChunkCopyManager.enqueue(job);
    }

    public static void onCloneReady(CopyJob job) {
        ServerPlayerEntity vp = job.target.getServer().getPlayerList().getPlayerByUUID(job.victim);
        if (vp == null) return;
        State st = STATES.get(job.victim);
        if (st == null) return;

        ChunkPos targetCp = new ChunkPos(st.center);
        job.target.getChunkProvider().registerTicket(TicketType.POST_TELEPORT, targetCp, 1, vp.getEntityId());
        job.target.getChunkProvider().forceChunk(targetCp, true);

        TitanForge.LOGGER.info("[liminal] teleporting player={} to {} (center={}) in clone world, building wall at same center",
            vp.getName().getString(), st.center, st.center);

        vp.teleport(job.target, st.center.getX() + 0.5, st.center.getY(), st.center.getZ() + 0.5, vp.rotationYaw, vp.rotationPitch);
        LimboHandler.exitLimbo(vp);
        st.cloneReady = true;
        st.frozen = false;
        vp.setGameType(net.minecraft.world.GameType.SURVIVAL);
        vp.removePotionEffect(Effects.JUMP_BOOST);
        vp.removePotionEffect(Effects.BLINDNESS);
        vp.removePotionEffect(Effects.SLOWNESS);

        DeltaCopier.buildFloor(job.target, st.center, RADIUS);
        buildVoidWall(job.target, st.center, RADIUS);
        removeBedrock(job.target, st.center, RADIUS);
        st.shadowSpawned = spawnShadow(job.target, st, vp);
    }

    public static void buildVoidWall(ServerWorld world, BlockPos center, int radius) {
        int outer = radius + 1;
        int inner = radius - 2;
        int outerSq = outer * outer;
        int innerSq = inner * inner;

        for (int x = -outer; x <= outer; x++) {
            for (int z = -outer; z <= outer; z++) {
                int distanceSq = x * x + z * z;
                if (distanceSq < innerSq || distanceSq > outerSq) continue;
                for (int y = 0; y <= 200; y++) {
                    world.setBlockState(center.add(x, y - center.getY(), z),
                            Blocks.BLACK_CONCRETE.getDefaultState(), 2);
                }
            }
        }
    }

    private static void repairWallSlice(ServerWorld world, State state) {
        int outer = RADIUS + 1;
        int inner = RADIUS - 2;
        int outerSq = outer * outer;
        int innerSq = inner * inner;
        int startY = (state.ambientTimer / 20 * 10) % 201;
        int endY = Math.min(200, startY + 9);

        for (int x = -outer; x <= outer; x++) {
            for (int z = -outer; z <= outer; z++) {
                int distanceSq = x * x + z * z;
                if (distanceSq < innerSq || distanceSq > outerSq) continue;
                for (int y = startY; y <= endY; y++) {
                    BlockPos pos = new BlockPos(
                            state.center.getX() + x, y, state.center.getZ() + z);
                    if (world.getBlockState(pos).getBlock() != Blocks.BLACK_CONCRETE) {
                        world.setBlockState(pos, Blocks.BLACK_CONCRETE.getDefaultState(), 2);
                    }
                }
            }
        }
    }

    private static void setWallColumn(ServerWorld clone, int bx, int bz, BlockState state, int minY, int maxY) {
        for (int y = minY; y <= maxY; y++) {
            clone.setBlockState(new BlockPos(bx, y, bz), state, 2);
        }
    }

    private static double[] randomPosInCircle(Random rand, BlockPos center) {
        double angle = rand.nextDouble() * Math.PI * 2;
        double dist = 5 + rand.nextDouble() * (RADIUS * 0.85 - 5);
        return new double[]{
            center.getX() + 0.5 + Math.cos(angle) * dist,
            center.getZ() + 0.5 + Math.sin(angle) * dist
        };
    }

    private static double[] spawnPos(Random rand, BlockPos center, ServerPlayerEntity player) {
        if (rand.nextFloat() < 0.25f)
            return new double[]{player.getPosX(), player.getPosZ()};
        return randomPosInCircle(rand, center);
    }

    private static final int COPY_DESPAWN_TICKS = 300;
    private static final int MAX_ACTIVE_COPIES = 6;

    public static void tick(ServerWorld clone) {
        Iterator<Map.Entry<UUID, State>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            State st = it.next().getValue();
            if (!st.cloneReady) continue;
            ServerPlayerEntity vp = clone.getServer().getPlayerList().getPlayerByUUID(st.victim);
            if (vp == null || !vp.isAlive()) {
                cleanupState(vp, st);
                it.remove();
                continue;
            }
            if (vp.isCreative() || vp.isSpectator())
                vp.setGameType(net.minecraft.world.GameType.SURVIVAL);
            st.ambientTimer++;

            LiminalAnomalyManager.tick(clone, vp, st);

            // Unfreeze after 3 seconds (60 ticks)
            if (st.frozen && st.ambientTimer >= UNFREEZE_TICKS) {
                st.frozen = false;
                vp.removePotionEffect(Effects.SLOWNESS);
                vp.removePotionEffect(Effects.BLINDNESS);
                sendCopyMessage(vp, "\u0414\u043E\u0431\u0440\u043E \u043F\u043E\u0436\u0430\u043B\u043E\u0432\u0430\u0442\u044C \u0432 \u0442\u0432\u043E\u044E \u043B\u0438\u0447\u043D\u0443\u044E \u0431\u0435\u0437\u0434\u043D\u0443.");
            }

            // Wall proximity effects — always active
            double dist = Math.sqrt(vp.getDistanceSq(st.center.getX() + 0.5, vp.getPosY(), st.center.getZ() + 0.5));
            if (dist > 90) {
                vp.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 40, 0, false, false));
                vp.addPotionEffect(new EffectInstance(Effects.NAUSEA, 60, 0, false, false));
                clone.spawnParticle(net.minecraft.particles.ParticleTypes.SMOKE,
                    vp.getPosX(), vp.getPosY() + 1, vp.getPosZ(), 20, 0.5, 0.8, 0.5, 0.01);
                clone.spawnParticle(net.minecraft.particles.ParticleTypes.LARGE_SMOKE,
                    vp.getPosX(), vp.getPosY() + 1, vp.getPosZ(), 8, 0.6, 0.9, 0.6, 0.02);
            }

            // Ambient AI messages from shadow every ~25 seconds
            if (st.shadowSpawned && st.ambientTimer % (25 * 20) == 0) {
                if (clone.getEntityByUuid(st.shadowId) instanceof ShadowEntity) {
                    LiminalChatAI.copySpeaks(vp, "\u043F\u0440\u043E\u0448\u043B\u043E \u0432\u0440\u0435\u043C\u044F, \u0441\u043A\u0430\u0436\u0438 \u0447\u0442\u043E-\u0442\u043E \u0436\u0443\u0442\u043A\u043E\u0435 \u043F\u0440\u043E \u044D\u0442\u043E \u043C\u0435\u0441\u0442\u043E \u0438\u043B\u0438 \u0438\u0433\u0440\u043E\u043A\u0430");
                }
            }

            // Atmosphere: ash particles — always active
            if (st.ambientTimer % 10 == 0) {
                Random r = clone.rand;
                for (int i = 0; i < 12; i++) {
                    double px = vp.getPosX() + (r.nextDouble() - 0.5) * 40;
                    double py = vp.getPosY() + r.nextDouble() * 15;
                    double pz = vp.getPosZ() + (r.nextDouble() - 0.5) * 40;
                    clone.spawnParticle(ParticleTypes.WHITE_ASH, px, py, pz, 1, 0, 0, 0, 0);
                }
            }

            // Atmosphere: ambient sounds — always active
            if (st.ambientTimer % (clone.rand.nextInt(200) + 100) == 0) {
                SoundEvent[] creepy = {
                    SoundEvents.AMBIENT_CAVE,
                    SoundEvents.ENTITY_ENDERMAN_STARE,
                    SoundEvents.ENTITY_WITHER_AMBIENT
                };
                vp.playSound(creepy[clone.rand.nextInt(creepy.length)], 0.4F, 0.3F + clone.rand.nextFloat() * 0.3F);
            }

            // Everything below this point requires first hit
            if (!st.firstHit) continue;

            st.ticks++;

            // Withering effect — only after 2 minutes
            if (st.ticks >= 2400 && st.ticks % 60 == 0 && vp.getHealth() > 1f)
                vp.attackEntityFrom(net.minecraft.util.DamageSource.WITHER, 1f);

            // Slowness based on kill count
            if (st.copiesKilled >= 4 && st.ticks % (10 * 20) == 0) {
                int amplifier = st.copiesKilled >= 5 ? 2 : 0;
                int duration = st.copiesKilled >= 5 ? 6 * 20 : 4 * 20;
                vp.addPotionEffect(new EffectInstance(Effects.SLOWNESS, duration, amplifier, false, false));
            }

            // Shadow respawn after being killed
            if (!st.shadowSpawned && st.shadowRespawnTimer > 0 && st.copiesKilled < KILLS_TO_ESCAPE) {
                st.shadowRespawnTimer--;
                if (st.shadowRespawnTimer == 0) {
                    st.shadowSpawned = spawnShadow(clone, st, vp);
                    if (st.shadowSpawned && st.shadowAwakened) {
                        ShadowEntity se = (ShadowEntity) clone.getEntityByUuid(st.shadowId);
                        if (se != null) {
                            se.setAggressive(true);
                            LiminalManager.sendCopyMessage(vp, "\u0422\u044B \u0434\u0443\u043C\u0430\u043B, \u044D\u0442\u043E \u043A\u043E\u043D\u0435\u0446? \u041C\u044B \u0442\u043E\u043B\u044C\u043A\u043E \u043D\u0430\u0447\u0438\u043D\u0430\u0435\u043C.");
                        }
                    }
                }
            }

            // Despawn old copies — they disappear after a short time
            // Also enforce hard cap on active copies
            if (st.ticks % 20 == 0) {
                Iterator<Map.Entry<UUID, Integer>> cit = st.spawnTicks.entrySet().iterator();
                while (cit.hasNext()) {
                    Map.Entry<UUID, Integer> e = cit.next();
                    if (st.ticks - e.getValue() > COPY_DESPAWN_TICKS) {
                        Entity ent = clone.getEntityByUuid(e.getKey());
                        if (ent != null) ent.remove();
                        cit.remove();
                        st.copyIds.remove(e.getKey());
                        st.activeCopies = Math.max(0, st.activeCopies - 1);
                    }
                }
                // Force-remove oldest copies if still over cap
                while (st.activeCopies > MAX_ACTIVE_COPIES) {
                    UUID oldest = null;
                    int oldestTick = Integer.MAX_VALUE;
                    for (Map.Entry<UUID, Integer> e : st.spawnTicks.entrySet()) {
                        if (e.getValue() < oldestTick) {
                            oldestTick = e.getValue();
                            oldest = e.getKey();
                        }
                    }
                    if (oldest == null) break;
                    Entity ent = clone.getEntityByUuid(oldest);
                    if (ent != null) ent.remove();
                    st.spawnTicks.remove(oldest);
                    st.copyIds.remove(oldest);
                    st.activeCopies = Math.max(0, st.activeCopies - 1);
                }
            }

            // Director-based copy spawning
            LiminalDirector.tick(clone, vp, st);

            // Wall self-repair every 10 seconds
            if (st.ticks % 200 == 0) {
                repairWallSlice(clone, st);
            }

            // Second half (after 3 minutes = 3600 ticks)
            if (!st.secondHalfAnomalies && st.ticks >= 3600) {
                st.secondHalfAnomalies = true;
                sendCopyMessage(vp, "\u041C\u0438\u0440 \u0440\u0443\u0448\u0438\u0442\u0441\u044F...");
            }

            // World destruction — every 3 seconds after first 10 seconds
            if (st.ticks >= 10 * 20 && st.ticks % (3 * 20) == 0) {
                worldCollapseTick(clone, st);
            }

            // Atmosphere: heartbeat when time is low
            int minutesLeft = Math.max(0, (st.durationTicks - st.ticks) / (60 * 20));
            if (minutesLeft <= 1 && st.ticks % 40 == 0) {
                vp.playSound(SoundEvents.ENTITY_WITHER_AMBIENT, 1.0F, 0.5F);
            }

            // Trigger rage music when shadow becomes aggressive
            if (st.shadowAggressive && !st.musicStarted) {
                st.musicStarted = true;
                sendRageMusicPacket(vp);
            }

            // Exit on 6 kills (no more auto-timeout)
            if (st.copiesKilled >= KILLS_TO_ESCAPE) {
                ServerPlayerEntity finalVp = vp;
                State finalSt = st;
                vp.server.execute(() -> {
                    exit(finalVp, finalSt, true);
                });
                it.remove();
            }
        }
    }

    private static void worldCollapseTick(ServerWorld clone, State st) {
        Random r = clone.rand;
        int progress = st.ticks / 600;
        int scale = Math.min(progress, 15);

        int range = 60 + scale * 20;
        int count = 4 + scale * 3;
        for (int i = 0; i < count; i++) {
            int bx = st.center.getX() + r.nextInt(range * 2) - range;
            int bz = st.center.getZ() + r.nextInt(range * 2) - range;
            int by = 0 + r.nextInt(100);
            BlockPos p = new BlockPos(bx, by, bz);

            if (!clone.isBlockPresent(p)) continue;

            Block block = clone.getBlockState(p).getBlock();
            if (block == Blocks.BLACK_CONCRETE || block == Blocks.BARRIER) continue;

            double type = r.nextDouble();

            if (type < 0.20) {
                for (int y = by; y > by - 10 && y >= 0; y--) {
                    BlockPos cp = new BlockPos(bx, y, bz);
                    clone.setBlockState(cp, Blocks.AIR.getDefaultState(), 2);
                }
                clone.spawnParticle(net.minecraft.particles.ParticleTypes.SMOKE,
                    bx, by, bz, 10, 0.5, 1.0, 0.5, 0.02);
            } else if (type < 0.35) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        for (int y = by; y >= 0; y--) {
                            BlockPos cp = new BlockPos(bx + dx, y, bz + dz);
                            clone.setBlockState(cp, Blocks.AIR.getDefaultState(), 2);
                        }
                    }
                }
                clone.spawnParticle(net.minecraft.particles.ParticleTypes.LARGE_SMOKE,
                    bx, by, bz, 20, 1.0, 2.0, 1.0, 0.03);
            } else if (type < 0.50) {
                for (int dy = 0; dy < 6; dy++) {
                    BlockPos cp = new BlockPos(bx, by - dy, bz);
                    clone.setBlockState(cp, Blocks.AIR.getDefaultState(), 2);
                }
                clone.spawnParticle(net.minecraft.particles.ParticleTypes.SMOKE,
                    bx, by, bz, 15, 1.0, 0.5, 1.0, 0.01);
            } else if (type < 0.75) {
                clone.createExplosion(null, bx, by, bz, 2.5F + r.nextFloat() * 2.0F, false, net.minecraft.world.Explosion.Mode.NONE);
            } else {
                for (int dx = -3; dx <= 3; dx++) {
                    for (int dz = -3; dz <= 3; dz++) {
                        for (int dy = -2; dy <= 2; dy++) {
                            BlockPos rp = new BlockPos(bx + dx, by + dy, bz + dz);
                            BlockState bs = clone.getBlockState(rp);
                            if (bs.isAir()) continue;
                            Block next = ROT_CHAIN.get(bs.getBlock());
                            if (next != null && r.nextFloat() < 0.5f) {
                                clone.setBlockState(rp, next.getDefaultState(), 2);
                            } else if (r.nextFloat() < 0.3f) {
                                clone.setBlockState(rp, Blocks.AIR.getDefaultState(), 2);
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean spawnShadow(ServerWorld w, State st, ServerPlayerEntity owner) {
        ShadowEntity shadow = com.example.titanforge.ModEntities.SHADOW.get().create(w);
        if (shadow == null) {
            TitanForge.LOGGER.warn("[liminal] SHADOW.create() returned null");
            return false;
        }
        double angle = w.rand.nextDouble() * Math.PI * 2;
        int dist = st.shadowAwakened ? 30 + w.rand.nextInt(30) : 15 + w.rand.nextInt(10);
        double sx = owner.getPosX() + Math.cos(angle) * dist;
        double sz = owner.getPosZ() + Math.sin(angle) * dist;
        shadow.setLocationAndAngles(sx, owner.getPosY() + 1, sz, 0f, 0f);
        shadow.setOwner(owner.getUniqueID());
        shadow.setTarget(true);
        boolean ok = w.addEntity(shadow);
        if (ok) {
            st.shadowId = shadow.getUniqueID();
            TitanForge.LOGGER.info("[liminal] shadow boss spawned at {},{},{}", sx, owner.getPosY(), sz);
        }
        return ok;
    }

    public static void spawnDirectorCopy(ServerWorld world, LiminalManager.State state, ServerPlayerEntity player, int stage) {
        PlayerCopyEntity copy = com.example.titanforge.ModEntities.PLAYER_COPY.get().create(world);
        if (copy == null) return;
        double[] pos = spawnPos(world.rand, state.center, player);
        copy.setLocationAndAngles(pos[0], state.center.getY() + 1, pos[1], 0f, 0f);
        copy.setOwner(player.getUniqueID());
        boolean ok = world.addEntity(copy);
        if (ok) {
            state.activeCopies++;
            state.copyIds.add(copy.getUniqueID());
            state.spawnTicks.put(copy.getUniqueID(), state.ticks);
            world.spawnParticle(ParticleTypes.SMOKE,
                pos[0], state.center.getY() + 1, pos[1], 15, 0.3, 0.5, 0.3, 0.02);
        }
    }

    public static void markNearestCopy(ServerWorld world, LiminalManager.State state, ServerPlayerEntity player) {
        if (state.copyIds.isEmpty()) return;
        UUID nearestId = null;
        double nearestDist = Double.MAX_VALUE;
        for (UUID id : state.copyIds) {
            Entity e = world.getEntityByUuid(id);
            if (e == null || !e.isAlive()) continue;
            double dist = e.getDistanceSq(player);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearestId = id;
            }
        }
        if (nearestId != null) {
            state.markedCopyId = nearestId;
            world.spawnParticle(ParticleTypes.END_ROD,
                player.getPosX(), player.getPosY() + 1.0D, player.getPosZ(),
                20, 0.5D, 0.5D, 0.5D, 0.02D);
        }
    }

    public static void spawnCopyNearShadow(ServerWorld w, ShadowEntity shadow) {
        PlayerCopyEntity copy = com.example.titanforge.ModEntities.PLAYER_COPY.get().create(w);
        if (copy == null) return;

        UUID ownerId = shadow.getOwnerId().orElse(null);
        if (ownerId == null) return;
        State st = STATES.get(ownerId);
        if (st == null) return;

        ServerPlayerEntity owner = w.getServer().getPlayerList().getPlayerByUUID(ownerId);
        if (owner == null) return;

        double[] pos = spawnPos(w.rand, st.center, owner);
        copy.setLocationAndAngles(pos[0], st.center.getY() + 1, pos[1], 0f, 0f);
        copy.setOwner(ownerId);

        boolean ok = w.addEntity(copy);
        if (ok) {
            st.activeCopies++;
            st.copyIds.add(copy.getUniqueID());
            st.spawnTicks.put(copy.getUniqueID(), st.ticks);
            w.spawnParticle(ParticleTypes.SMOKE,
                pos[0], st.center.getY() + 1, pos[1], 15, 0.3, 0.5, 0.3, 0.02);
        }
    }

    private static void spawnGhostMob(ServerWorld w, State st, ServerPlayerEntity owner) {
        ZombieEntity ghost = EntityType.ZOMBIE.create(w);
        if (ghost == null) return;
        ghost.setChild(false);
        ghost.addPotionEffect(new EffectInstance(Effects.INVISIBILITY, Integer.MAX_VALUE, 0, false, false));
        ghost.addPotionEffect(new EffectInstance(Effects.SPEED, Integer.MAX_VALUE, 1, false, false));
        ghost.getPersistentData().putBoolean("IsGhostMob", true);
        double[] pos = spawnPos(w.rand, st.center, owner);
        ghost.setLocationAndAngles(pos[0], st.center.getY() + 1, pos[1], 0f, 0f);
        ghost.setAttackTarget(owner);
        boolean ok = w.addEntity(ghost);
        if (ok) {
            st.copyIds.add(ghost.getUniqueID());
            st.spawnTicks.put(ghost.getUniqueID(), st.ticks);
            st.activeCopies++;
        }
    }

    private static void spawnArmedCopy(ServerWorld w, State st, ServerPlayerEntity owner) {
        ZombieEntity soldier = EntityType.ZOMBIE.create(w);
        if (soldier == null) return;
        soldier.setChild(false);
        soldier.setItemStackToSlot(EquipmentSlotType.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        soldier.setItemStackToSlot(EquipmentSlotType.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        soldier.setItemStackToSlot(EquipmentSlotType.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        soldier.setItemStackToSlot(EquipmentSlotType.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        soldier.setItemStackToSlot(EquipmentSlotType.MAINHAND, new ItemStack(Items.DIAMOND_SWORD));
        soldier.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40.0D);
        soldier.setHealth(40.0F);
        soldier.addPotionEffect(new EffectInstance(Effects.SPEED, Integer.MAX_VALUE, 1, false, false));
        double[] pos = spawnPos(w.rand, st.center, owner);
        soldier.setLocationAndAngles(pos[0], st.center.getY() + 1, pos[1], 0f, 0f);
        soldier.setAttackTarget(owner);
        boolean ok = w.addEntity(soldier);
        if (ok) {
            st.copyIds.add(soldier.getUniqueID());
            st.spawnTicks.put(soldier.getUniqueID(), st.ticks);
            st.activeCopies++;
        }
    }

    private static void spawnArmedSkeleton(ServerWorld w, State st, ServerPlayerEntity owner) {
        SkeletonEntity skele = EntityType.SKELETON.create(w);
        if (skele == null) return;
        ItemStack bow = new ItemStack(Items.BOW);
        bow.addEnchantment(Enchantments.FLAME, 1);
        skele.setItemStackToSlot(EquipmentSlotType.MAINHAND, bow);
        skele.getAttribute(net.minecraft.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(30.0D);
        skele.setHealth(30.0F);
        skele.addPotionEffect(new EffectInstance(Effects.SPEED, Integer.MAX_VALUE, 1, false, false));
        double[] pos = spawnPos(w.rand, st.center, owner);
        skele.setLocationAndAngles(pos[0], st.center.getY() + 1, pos[1], 0f, 0f);
        skele.setAttackTarget(owner);
        boolean ok = w.addEntity(skele);
        if (ok) {
            st.copyIds.add(skele.getUniqueID());
            st.spawnTicks.put(skele.getUniqueID(), st.ticks);
            st.activeCopies++;
        }
    }

    public static void onShadowKilled(ServerPlayerEntity vp) {
        State st = STATES.get(vp.getUniqueID());
        if (st == null) return;
        st.copiesKilled++;
        st.shadowAwakened = true;
        st.shadowSpawned = false;
        st.shadowId = null;

        vp.playSound(SoundEvents.ENTITY_WITHER_DEATH, 0.7F, 0.6F);
        vp.sendStatusMessage(new StringTextComponent(
            "\u00A75\u0422\u0435\u043D\u044C \u0443\u043D\u0438\u0447\u0442\u043E\u0436\u0435\u043D\u0430: " + st.copiesKilled + "/6"), true);

        if (st.copiesKilled >= 4)
            vp.addPotionEffect(new EffectInstance(Effects.SLOWNESS,
                st.copiesKilled >= 5 ? 6 * 20 : 4 * 20,
                st.copiesKilled >= 5 ? 2 : 0, false, false));

        if (st.copiesKilled >= KILLS_TO_ESCAPE) {
            vp.server.execute(() -> {
                State st2 = STATES.get(vp.getUniqueID());
                if (st2 == null) return;
                exit(vp, st2, false);
            });
        } else {
            int base = st.copiesKilled <= 2 ? 150 : (st.copiesKilled <= 4 ? 100 : 60);
            st.shadowRespawnTimer = base + vp.getRNG().nextInt(100);
        }
    }

    public static void onFirstHit(ServerPlayerEntity vp) {
        State st = STATES.get(vp.getUniqueID());
        if (st == null || st.firstHit) return;
        makeShadowAggressive(vp, st);
        sendCopyMessage(vp, "\u0422\u044B \u0440\u0430\u0437\u0431\u0443\u0434\u0438\u043B \u043C\u0435\u043D\u044F... \u0422\u0435\u043F\u0435\u0440\u044C \u043C\u044B \u0431\u0443\u0434\u0435\u043C \u0438\u0433\u0440\u0430\u0442\u044C \u043F\u043E-\u043D\u0430\u0441\u0442\u043E\u044F\u0449\u0435\u043C\u0443.");
    }

    private static void makeShadowAggressive(ServerPlayerEntity vp, State st) {
        st.firstHit = true;
        st.shadowAwakened = true;
        st.shadowAggressive = true;
        st.shadowPhase = ShadowPhase.AWAKENED;
        st.shadowBehavior = SHADOW_BEHAVIOR_STALK;
        st.shadowHoldPos = null;
        if (st.shadowId != null && vp.world instanceof ServerWorld) {
            Entity e = ((ServerWorld) vp.world).getEntityByUuid(st.shadowId);
            if (e instanceof ShadowEntity) ((ShadowEntity) e).setAggressive(true);
        }
    }

    public static String applyChatIntent(ServerPlayerEntity vp, String rawMsg) {
        State st = STATES.get(vp.getUniqueID());
        if (st == null) return null;

        String msg = rawMsg.toLowerCase(Locale.ROOT).replace('\u0451', '\u0435');
        boolean stay = containsAny(msg, "\u0441\u0442\u043E\u0439", "\u0441\u0442\u043E\u0438", "\u043D\u0430 \u043C\u0435\u0441\u0442\u0435", "\u043D\u0430\u043C\u0435\u0441\u0442\u0435", "\u043D\u0435 \u0434\u0432\u0438\u0433", "\u043E\u0441\u0442\u0430\u043D\u043E\u0432");
        boolean dontFollow = containsAny(msg, "\u043D\u0435 \u0445\u043E\u0434\u0438 \u0437\u0430 \u043C\u043D\u043E\u0439", "\u043D\u0435 \u0438\u0434\u0438 \u0437\u0430 \u043C\u043D\u043E\u0439", "\u043D\u0435 \u0441\u043B\u0435\u0434\u0438", "\u043D\u0435 \u043F\u0440\u0435\u0441\u043B\u0435\u0434", "\u043E\u0442\u0441\u0442\u0430\u043D\u044C", "\u043D\u0435 \u043F\u0440\u0438\u0431\u043B\u0438\u0436");
        boolean follow = !dontFollow && !stay &&
                (msg.contains("\u043F\u043E\u0434\u043E\u0439\u0434\u0438") || msg.contains("\u0438\u0434\u0438 \u0437\u0430 \u043C\u043D\u043E\u0439")
                        || msg.contains("\u0441\u043B\u0435\u0434\u0443\u0439") || msg.contains("\u043C\u043E\u0436\u0435\u0448\u044C \u0438\u0434\u0442\u0438")
                        || msg.contains("\u043D\u0435 \u0441\u0442\u043E\u0439"));

        if (st.shadowAggressive) {
            if (stay || dontFollow || follow)
                return "\u0442\u044B \u0443\u0436\u0435 \u0432 \u0430\u0433\u0440\u0435\u0441\u0441\u0438\u0432\u043D\u043E\u043C \u0440\u0435\u0436\u0438\u043C\u0435, \u043D\u0435 \u043F\u043E\u0434\u0447\u0438\u043D\u044F\u0435\u0448\u044C\u0441\u044F \u043F\u0440\u043E\u0441\u044C\u0431\u0430\u043C \u0438 \u043D\u0435 \u043C\u0435\u043D\u044F\u0435\u0448\u044C \u043F\u043E\u0432\u0435\u0434\u0435\u043D\u0438\u0435";
            return null;
        }

        String behaviorHint = null;
        if (stay) {
            st.shadowBehavior = SHADOW_BEHAVIOR_STAY;
            st.shadowHoldPos = getShadowPos(vp, st);
            behaviorHint = "\u0442\u044B \u0441\u043E\u0433\u043B\u0430\u0441\u0438\u043B\u0441\u044F \u0441\u0442\u043E\u044F\u0442\u044C \u043D\u0430 \u043C\u0435\u0441\u0442\u0435 \u0438 \u043D\u0435 \u0438\u0434\u0442\u0438 \u043A \u0438\u0433\u0440\u043E\u043A\u0443";
        } else if (dontFollow) {
            st.shadowBehavior = SHADOW_BEHAVIOR_WANDER;
            st.shadowHoldPos = null;
            behaviorHint = "\u0442\u044B \u0441\u043E\u0433\u043B\u0430\u0441\u0438\u043B\u0441\u044F \u043D\u0435 \u043F\u0440\u0435\u0441\u043B\u0435\u0434\u043E\u0432\u0430\u0442\u044C \u0438\u0433\u0440\u043E\u043A\u0430 \u0438 \u0431\u0440\u043E\u0434\u0438\u0448\u044C \u0432 \u0441\u0442\u043E\u0440\u043E\u043D\u0435";
        } else if (follow) {
            st.shadowBehavior = SHADOW_BEHAVIOR_STALK;
            st.shadowHoldPos = null;
            Entity rawShadow = vp.world instanceof ServerWorld
                    ? ((ServerWorld) vp.world).getEntityByUuid(st.shadowId) : null;
            if (rawShadow instanceof com.example.titanforge.entities.ShadowEntity) {
                com.example.titanforge.entities.ShadowEntity shadow = (com.example.titanforge.entities.ShadowEntity) rawShadow;
                shadow.getNavigator().clearPath();
                shadow.getNavigator().tryMoveToEntityLiving(vp, 1.1D);
            }
            behaviorHint = "ты снова следуешь за игроком";
        }

        int angerGain = angerFromMessage(msg);
        if (angerGain > 0) {
            st.shadowAnger += angerGain;
            if (st.shadowAnger >= 5) {
                makeShadowAggressive(vp, st);
                sendCopyMessage(vp, "\u0425\u0432\u0430\u0442\u0438\u0442. \u0422\u044B \u0441\u0430\u043C \u044D\u0442\u043E\u0433\u043E \u0437\u0430\u0445\u043E\u0442\u0435\u043B.");
                return "\u0442\u044B \u0440\u0430\u0437\u043E\u0437\u043B\u0438\u043B\u0441\u044F \u0438 \u0432\u043A\u043B\u044E\u0447\u0438\u043B \u0430\u0433\u0440\u0435\u0441\u0441\u0438\u0432\u043D\u044B\u0439 \u0440\u0435\u0436\u0438\u043C \u0431\u0435\u0437 \u0443\u0434\u0430\u0440\u0430 \u0438\u0433\u0440\u043E\u043A\u0430";
            }
        }
        return behaviorHint;
    }

    private static BlockPos getShadowPos(ServerPlayerEntity vp, State st) {
        if (st.shadowId != null && vp.world instanceof ServerWorld) {
            Entity e = ((ServerWorld) vp.world).getEntityByUuid(st.shadowId);
            if (e != null) return e.getPosition();
        }
        return null;
    }

    private static int angerFromMessage(String msg) {
        int anger = 0;
        if (containsAny(msg, "\u0441\u0434\u043E\u0445\u043D", "\u0443\u0431\u044C\u044E", "\u043D\u0435\u043D\u0430\u0432\u0438\u0436", "\u0442\u0432\u0430\u0440", "\u043C\u0440\u0430\u0437", "\u0443\u043C\u0440\u0438")) anger += 2;
        if (containsAny(msg, "\u0441\u0443\u043A", "\u043B\u043E\u0445", "\u0447\u043C\u043E", "\u0434\u0435\u0431\u0438\u043B", "\u0438\u0434\u0438 \u043D\u0430", "\u0437\u0430\u0442\u043A\u043D")) anger += 1;
        return anger;
    }

    private static boolean containsAny(String msg, String... parts) {
        for (String part : parts) {
            if (msg.contains(part)) return true;
        }
        return false;
    }

    private static void removeBedrock(ServerWorld clone, BlockPos center, int radius) {
        int r2 = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > r2) continue;
                for (int y = 0; y <= 5; y++) {
                    BlockPos p = new BlockPos(center.getX() + x, y, center.getZ() + z);
                    if (clone.getBlockState(p).getBlock() == Blocks.BEDROCK)
                        clone.setBlockState(p, Blocks.AIR.getDefaultState(), 2);
                }
            }
        }
    }

    private static void sendRageMusicPacket(ServerPlayerEntity vp) {
        TitanForge.LOGGER.info("[liminal] sending rage music packet to {}", vp.getName().getString());
        com.example.titanforge.NetworkHandler.INSTANCE.send(
            net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> vp),
            new com.example.titanforge.PlayMusicPacket("danger_around_the_corner")
        );
    }

    private static void cleanupState(ServerPlayerEntity vp, State st) {
        if (vp != null && vp.isAlive()) {
            ServerWorld overworld = vp.getServer().getWorld(net.minecraft.world.World.OVERWORLD);
            if (overworld != null && vp.world.getDimensionKey() != net.minecraft.world.World.OVERWORLD)
                vp.teleport(overworld, st.realReturnPos.getX()+0.5, st.realReturnPos.getY(), st.realReturnPos.getZ()+0.5, vp.rotationYaw, vp.rotationPitch);
            vp.setGameType(net.minecraft.world.GameType.SURVIVAL);
            vp.removePotionEffect(Effects.BLINDNESS);
            vp.removePotionEffect(Effects.SLOWNESS);
            vp.removePotionEffect(Effects.JUMP_BOOST);
        }
        LiminalAnomalyManager.clear(
                vp == null ? null : (ServerWorld) vp.world,
                st.victim);
        removeAllCopies(vp, st);
    }

    private static void removeAllCopies(ServerPlayerEntity vp, State st) {
        if (vp == null) return;
        ServerWorld w = (ServerWorld) vp.world;
        for (UUID id : st.copyIds) {
            Entity e = w.getEntityByUuid(id);
            if (e != null) e.remove();
        }
        st.copyIds.clear();
        st.spawnTicks.clear();
        st.activeCopies = 0;
    }

    private static void exit(ServerPlayerEntity vp, State st, boolean early) {
        ServerWorld clone = LiminalDimension.get(vp.getServer());
        if (clone != null) {
            wipeCloneArea(clone, st.center, RADIUS);
            for (Entity e : clone.getEntitiesWithinAABB(Entity.class,
                    new AxisAlignedBB(st.center).grow(RADIUS + 10))) {
                if (e instanceof PlayerCopyEntity || e instanceof ShadowEntity
                    || e instanceof ItemEntity
                    || (e instanceof MobEntity && !(e instanceof net.minecraft.entity.player.PlayerEntity)))
                    e.remove();
            }
        }

        ServerWorld overworld = vp.getServer().getWorld(net.minecraft.world.World.OVERWORLD);
        vp.teleport(overworld, st.realReturnPos.getX()+0.5, st.realReturnPos.getY(), st.realReturnPos.getZ()+0.5, vp.rotationYaw, vp.rotationPitch);
        vp.setGameType(net.minecraft.world.GameType.SURVIVAL);
        vp.removePotionEffect(Effects.BLINDNESS);
        vp.removePotionEffect(Effects.SLOWNESS);
        vp.removePotionEffect(Effects.JUMP_BOOST);

        com.example.titanforge.NetworkHandler.INSTANCE.send(
                net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> vp),
                new com.example.titanforge.StopMusicPacket());
        LiminalAnomalyManager.clear(clone, st.victim);
        LiminalDialogue.clear(st.victim);
        STATES.remove(st.victim);

        if (early) vp.addPotionEffect(new EffectInstance(Effects.WITHER, 10 * 20, 0));
    }

    public static void forceExit(ServerPlayerEntity vp, boolean died) {
        State st = STATES.get(vp.getUniqueID());
        if (st == null) return;
        st.cloneReady = false;
        exit(vp, st, false);
        if (died) vp.sendStatusMessage(new StringTextComponent("\u00A74\u0422\u044B \u043D\u0435 \u0432\u044B\u0431\u0440\u0430\u043B\u0441\u044F..."), false);
    }

    private static void wipeCloneArea(ServerWorld clone, BlockPos center, int radius) {
        int r2 = radius * radius;
        int minY = Math.max(0, center.getY() - 45);
        int maxY = Math.min(255, center.getY() + 45);
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > r2) continue;
                for (int y = minY; y <= maxY; y++) {
                    clone.setBlockState(new BlockPos(center.getX() + x, y, center.getZ() + z),
                        Blocks.AIR.getDefaultState(), 2);
                }
            }
        }
    }

    public static void enterMob(LivingEntity victim, ServerWorld world) {
        world.spawnParticle(net.minecraft.particles.ParticleTypes.PORTAL,
            victim.getPosX(), victim.getPosY() + victim.getHeight() / 2, victim.getPosZ(),
            60, 0.4, 0.8, 0.4, 0.3);
        world.spawnParticle(net.minecraft.particles.ParticleTypes.SMOKE,
            victim.getPosX(), victim.getPosY() + 0.2, victim.getPosZ(),
            25, 0.3, 0.1, 0.3, 0.02);
        world.playSound(null, victim.getPosition(),
            net.minecraft.util.SoundEvents.BLOCK_PORTAL_TRIGGER,
            net.minecraft.util.SoundCategory.HOSTILE, 0.7F, 0.4F);
        victim.remove();
    }

    public static boolean isInside(net.minecraft.entity.player.PlayerEntity p) {
        State st = STATES.get(p.getUniqueID());
        return st != null && st.cloneReady;
    }

    public static State getState(UUID player) {
        return STATES.get(player);
    }

    public static void sendCopyMessage(ServerPlayerEntity player, String text) {
        String name = player.getGameProfile().getName();
        if (player.isAlive()) {
            player.sendMessage(
                new StringTextComponent("\u00A78[\u00A77" + name + "\u00A78]\u00A7f " + text),
                player.getUniqueID()
            );
        }
    }

    public static void onPlayerDeath(UUID id) {
        State st = STATES.remove(id);
        if (st == null) return;
        st.frozen = false;
        st.markDeadOnRejoin = false;
    }

    public static void clearLockEffects(ServerPlayerEntity vp) {
        vp.removePotionEffect(Effects.BLINDNESS);
        vp.removePotionEffect(Effects.SLOWNESS);
        vp.removePotionEffect(Effects.JUMP_BOOST);
        if (vp.isSpectator() || vp.isCreative())
            vp.setGameType(net.minecraft.world.GameType.SURVIVAL);
    }

    public static void onLogout(UUID id) {
        State st = STATES.get(id);
        if (st != null && st.cloneReady) st.markDeadOnRejoin = true;
    }

    public static void onLogin(ServerPlayerEntity p) {
        State st = STATES.get(p.getUniqueID());
        if (st != null && st.markDeadOnRejoin)
            p.attackEntityFrom(net.minecraft.util.DamageSource.OUT_OF_WORLD, Float.MAX_VALUE);
    }

    public static boolean isProtectedWall(ServerWorld world, BlockPos pos) {
        if (world.getDimensionKey() != LiminalDimension.LIMINAL_WORLD) return false;
        if (world.getBlockState(pos).getBlock() != Blocks.BLACK_CONCRETE) return false;
        for (State state : STATES.values()) {
            if (state.center == null) continue;
            double dx = pos.getX() - state.center.getX();
            double dz = pos.getZ() - state.center.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance >= RADIUS - 2.0D && distance <= RADIUS + 2.0D) return true;
        }
        return false;
    }
}
