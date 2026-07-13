package com.example.titanforge.liminal;

import com.google.common.collect.ImmutableMap;
import com.example.titanforge.ModBlocks;
import com.example.titanforge.NetworkHandler;
import com.example.titanforge.PlayMusicPacket;
import com.example.titanforge.StopMusicPacket;
import com.example.titanforge.TitanForge;
import com.example.titanforge.entities.PlayerCopyEntity;
import com.example.titanforge.entities.ShadowEntity;
import com.example.titanforge.liminal.ai.LiminalDialogue;
import com.example.titanforge.liminal.chat.LiminalChatAI;
import com.example.titanforge.liminal.copy.ChunkCopyManager;
import com.example.titanforge.liminal.copy.CopyJob;
import com.example.titanforge.liminal.copy.DeltaCopier;
import com.example.titanforge.liminal.reward.RewardShadowManager;
import com.example.titanforge.liminal.screen.LimboHandler;
import com.example.titanforge.liminal.screen.LiminalWallParticles;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
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
    private static final int KILLS_TO_ESCAPE = 6;
    private static final int UNFREEZE_TICKS = 60;
    private static final java.util.Set<java.util.UUID> PENDING_DEATH_EXITS =
        java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static void requestDeathExit(ServerPlayerEntity player) {
        if (player != null) {
            PENDING_DEATH_EXITS.add(player.getUniqueID());
        }
    }

    public static void flushPendingDeathExits(
            net.minecraft.server.MinecraftServer server) {
        if (PENDING_DEATH_EXITS.isEmpty()) return;

        for (java.util.UUID id : new java.util.HashSet<>(PENDING_DEATH_EXITS)) {
            PENDING_DEATH_EXITS.remove(id);
            ServerPlayerEntity player = server.getPlayerList()
                .getPlayerByUUID(id);
            if (player == null) continue;

            State state = STATES.remove(id);
            if (state == null) continue;

            ServerWorld liminal = LiminalDimension.get(server);
            if (liminal != null) {
                cleanupRuntimeState(liminal, state);
                queueArenaCleanup(liminal, state);
            }
            clearLockEffects(player);
            NetworkHandler.sendTo(player, new StopMusicPacket());
        }
    }

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
        public int copiesKilled = 0;
        public int activeCopies = 0;
        public int activeArmedCopies = 0;
        public boolean cloneReady = false;
        public boolean shadowSpawned = false;
        public boolean frozen = false;
        public boolean secondHalfAnomalies = false;
        public boolean shadowAggressive = false;
        public boolean shadowAwakened = false;
        public boolean firstHit = false;
        public boolean musicStarted = false;
        public UUID sessionId;
        public BlockPos sourceCenter;
        public boolean cleanupQueued;
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
        public int collapseCooldown;
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    public static void applyLiminalHit(LivingEntity target) {
        target.addPotionEffect(new EffectInstance(Effects.BLINDNESS, Integer.MAX_VALUE, 0, false, false));
        target.addPotionEffect(new EffectInstance(Effects.SLOWNESS, Integer.MAX_VALUE, 255, false, false));
        target.addPotionEffect(new EffectInstance(Effects.JUMP_BOOST, Integer.MAX_VALUE, 128, false, false));
    }

    public static void enter(LivingEntity victim, ServerPlayerEntity owner) {
        if (!(victim instanceof ServerPlayerEntity)) {
            enterMob(victim, (ServerWorld) victim.world);
            return;
        }

        ServerPlayerEntity player = (ServerPlayerEntity) victim;
        ServerWorld source = (ServerWorld) player.world;
        ServerWorld liminal = LiminalDimension.get(player.getServer());
        if (liminal == null) {
            player.sendStatusMessage(new StringTextComponent("\u00A7c\u041B\u0438\u043C\u0438\u043D\u0430\u043B\u044C\u043D\u043E\u0435 \u0438\u0437\u043C\u0435\u0440\u0435\u043D\u0438\u0435 \u043D\u0435 \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043D\u043E (\u043D\u0443\u0436\u0435\u043D \u043D\u043E\u0432\u044B\u0439 \u043C\u0438\u0440)"), true);
            return;
        }

        UUID playerId = player.getUniqueID();
        if (LiminalArenaCleaner.isCleaning(playerId)) {
            player.sendStatusMessage(new StringTextComponent(
                    "\u00A7e\u041B\u0438\u043C\u0438\u043D\u0430\u043B \u0435\u0449\u0451 \u043E\u0447\u0438\u0449\u0430\u0435\u0442\u0441\u044F. \u041F\u043E\u043F\u0440\u043E\u0431\u0443\u0439 \u0447\u0435\u0440\u0435\u0437 \u043D\u0435\u0441\u043A\u043E\u043B\u044C\u043A\u043E \u0441\u0435\u043A\u0443\u043D\u0434."), true);
            return;
        }

        State previous = STATES.remove(playerId);
        if (previous != null) {
            cleanupRuntimeState(liminal, previous);
        }
        ChunkCopyManager.cancelFor(playerId);

        State state = new State();
        state.sessionId = UUID.randomUUID();
        state.victim = playerId;
        state.sourceCenter = player.getPosition().toImmutable();
        state.center = LiminalArenaSlots.center(playerId);
        state.realReturnPos = player.getPosition().toImmutable();
        state.frozen = true;
        STATES.put(playerId, state);

        TitanForge.LOGGER.info("[liminal] enter: player={} arenaCenter={} sourceCenter={} realReturn={}",
            player.getName().getString(), state.center, state.sourceCenter, state.realReturnPos);

        applyLiminalHit(player);
        LimboHandler.enterLimbo(player);

        UUID expectedSession = state.sessionId;
        LiminalArenaCleaner.start(liminal, playerId,
                state.center, RADIUS + 4, () -> {
            State current = STATES.get(playerId);
            ServerPlayerEntity online = player.getServer()
                    .getPlayerList().getPlayerByUUID(playerId);

            if (current == null
                    || !expectedSession.equals(current.sessionId)
                    || online == null
                    || !online.isAlive()) {
                return;
            }

            CopyJob job = new CopyJob(playerId,
                    source, liminal,
                    current.sourceCenter,
                    current.center,
                    RADIUS);
            ChunkCopyManager.enqueue(job);
        });
    }

    public static void onCloneReady(CopyJob job) {
        ServerPlayerEntity player = job.target.getServer().getPlayerList().getPlayerByUUID(job.victim);
        if (player == null) return;
        State st = STATES.get(job.victim);
        if (st == null) return;

        BlockPos center = job.targetCenter;

        ChunkPos targetCp = new ChunkPos(st.center);
        job.target.getChunkProvider().registerTicket(TicketType.POST_TELEPORT, targetCp, 1, player.getEntityId());
        job.target.getChunkProvider().forceChunk(targetCp, true);
        TitanForge.LOGGER.info("[liminal] forced chunk ticket for player={} at {}", player.getName().getString(), targetCp);

        TitanForge.LOGGER.info("[liminal] teleporting player={} to arena center {}",
            player.getName().getString(), center);

        buildVoidWall(job.target, center, RADIUS);
        removeBedrock(job.target, center, RADIUS);

        BlockPos safeSpawn = findSafeSpawn(
                job.target,
                center,
                job.sourceCenter.getY());

        player.teleport(job.target,
                safeSpawn.getX() + 0.5D,
                safeSpawn.getY(),
                safeSpawn.getZ() + 0.5D,
                player.rotationYaw,
                player.rotationPitch);

        player.setMotion(0.0D, 0.0D, 0.0D);
        player.fallDistance = 0.0F;
        player.velocityChanged = true;

        LimboHandler.exitLimbo(player);
        st.cloneReady = true;
        st.frozen = false;
        player.setGameType(net.minecraft.world.GameType.SURVIVAL);
        player.removePotionEffect(Effects.JUMP_BOOST);
        player.removePotionEffect(Effects.BLINDNESS);
        player.removePotionEffect(Effects.SLOWNESS);

        st.shadowSpawned = spawnShadow(job.target, st, player);
        startCalmMusic(player, st);
    }

    private static BlockPos findSafeSpawn(ServerWorld world,
                                           BlockPos center,
                                           int preferredY) {
        for (int radius = 0; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (radius > 0
                        && Math.abs(dx) != radius
                        && Math.abs(dz) != radius) continue;

                    for (int offset = 0; offset <= 48; offset++) {
                        int[] ys = offset == 0
                            ? new int[]{preferredY}
                            : new int[]{preferredY + offset, preferredY - offset};

                        for (int y : ys) {
                            if (y < 2 || y > 253) continue;

                            BlockPos feet = new BlockPos(
                                center.getX() + dx, y, center.getZ() + dz);
                            BlockPos head = feet.up();
                            BlockPos floor = feet.down();

                            BlockState floorState = world.getBlockState(floor);
                            Block floorBlock = floorState.getBlock();

                            boolean dangerous =
                                floorBlock == Blocks.LAVA
                                || floorBlock == Blocks.FIRE
                                || floorBlock == Blocks.SOUL_FIRE
                                || floorBlock == Blocks.CACTUS
                                || floorBlock == Blocks.MAGMA_BLOCK
                                || floorBlock == Blocks.CAMPFIRE
                                || floorBlock == ModBlocks.YELLOW_DECAY_BLOCK.get()
                                || floorBlock == ModBlocks.RED_DECAY_BLOCK.get();

                            boolean free =
                                world.isAirBlock(feet)
                                && world.isAirBlock(head)
                                && world.getFluidState(feet).isEmpty()
                                && world.getFluidState(head).isEmpty();

                            boolean solidFloor =
                                !floorState.isAir()
                                && floorState.getMaterial().isSolid();

                            if (free && solidFloor && !dangerous) {
                                return feet;
                            }
                        }
                    }
                }
            }
        }

        BlockPos fallback = new BlockPos(
            center.getX(), Math.max(3, Math.min(252, preferredY)),
            center.getZ());

        world.setBlockState(fallback.down(),
            Blocks.OBSIDIAN.getDefaultState(), 18);
        world.setBlockState(fallback,
            Blocks.AIR.getDefaultState(), 18);
        world.setBlockState(fallback.up(),
            Blocks.AIR.getDefaultState(), 18);

        return fallback;
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

    private static void spawnWallAmbient(ServerWorld world, State state) {
        Random r = world.rand;
        int angleStep = r.nextInt(360);
        int wallRadius = RADIUS + 1;
        double rad = Math.toRadians(angleStep);
        double wx = state.center.getX() + Math.cos(rad) * wallRadius;
        double wz = state.center.getZ() + Math.sin(rad) * wallRadius;
        double wy = 10 + r.nextInt(180);

        net.minecraft.particles.IParticleData[] dark = {
            ParticleTypes.SMOKE,
            ParticleTypes.LARGE_SMOKE,
            ParticleTypes.SOUL_FIRE_FLAME
        };
        net.minecraft.particles.IParticleData p = dark[r.nextInt(dark.length)];
        world.spawnParticle(p, wx + 0.5, wy, wz + 0.5, 1, 0.3, 0.3, 0.3, 0.01);

        if (r.nextInt(3) == 0) {
            rad = Math.toRadians((angleStep + 120 + r.nextInt(120)) % 360);
            wx = state.center.getX() + Math.cos(rad) * wallRadius;
            wz = state.center.getZ() + Math.sin(rad) * wallRadius;
            wy = 10 + r.nextInt(180);
            p = dark[r.nextInt(dark.length)];
            world.spawnParticle(p, wx + 0.5, wy, wz + 0.5, 1, 0.3, 0.3, 0.3, 0.01);
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
        // Loading progress for non-ready states
        for (State st : STATES.values()) {
            if (!st.cloneReady && st.ambientTimer % 20 == 0) {
                ServerPlayerEntity p = clone.getServer().getPlayerList().getPlayerByUUID(st.victim);
                if (p != null && p.world == clone) {
                    LimboHandler.tickProgress(p);
                }
            }
        }

        Iterator<Map.Entry<UUID, State>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            State st = it.next().getValue();
            if (!st.cloneReady) continue;
            ServerPlayerEntity player = clone.getServer().getPlayerList().getPlayerByUUID(st.victim);
            if (player == null || !player.isAlive()) {
                it.remove();
                queueArenaCleanup(clone, st);
                continue;
            }
            if (player.world != clone) {
                it.remove();
                queueArenaCleanup(clone, st);
                clearLockEffects(player);
                continue;
            }
            if (player.isCreative() || player.isSpectator())
                player.setGameType(net.minecraft.world.GameType.SURVIVAL);
            st.ambientTimer++;

            LiminalAnomalyManager.tick(clone, player, st);

            // Unfreeze after 3 seconds (60 ticks)
            if (st.frozen && st.ambientTimer >= UNFREEZE_TICKS) {
                st.frozen = false;
                player.removePotionEffect(Effects.SLOWNESS);
                player.removePotionEffect(Effects.BLINDNESS);
                sendCopyMessage(player, "\u0414\u043E\u0431\u0440\u043E \u043F\u043E\u0436\u0430\u043B\u043E\u0432\u0430\u0442\u044C \u0432 \u0442\u0432\u043E\u044E \u043B\u0438\u0447\u043D\u0443\u044E \u0431\u0435\u0437\u0434\u043D\u0443.");
            }

            // Wall proximity effects — always active
            double dist = Math.sqrt(player.getDistanceSq(st.center.getX() + 0.5, player.getPosY(), st.center.getZ() + 0.5));
            if (dist > 90) {
                player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 40, 0, false, false));
                player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 60, 0, false, false));
                clone.spawnParticle(net.minecraft.particles.ParticleTypes.SMOKE,
                    player.getPosX(), player.getPosY() + 1, player.getPosZ(), 20, 0.5, 0.8, 0.5, 0.01);
                clone.spawnParticle(net.minecraft.particles.ParticleTypes.LARGE_SMOKE,
                    player.getPosX(), player.getPosY() + 1, player.getPosZ(), 8, 0.6, 0.9, 0.6, 0.02);
            }

            // Ambient AI messages from shadow every ~25 seconds
            if (st.shadowSpawned && st.ambientTimer % (25 * 20) == 0) {
                if (clone.getEntityByUuid(st.shadowId) instanceof ShadowEntity) {
                    LiminalChatAI.copySpeaks(player, "\u043F\u0440\u043E\u0448\u043B\u043E \u0432\u0440\u0435\u043C\u044F, \u0441\u043A\u0430\u0436\u0438 \u0447\u0442\u043E-\u0442\u043E \u0436\u0443\u0442\u043A\u043E\u0435 \u043F\u0440\u043E \u044D\u0442\u043E \u043C\u0435\u0441\u0442\u043E \u0438\u043B\u0438 \u0438\u0433\u0440\u043E\u043A\u0430");
                }
            }

            // Atmosphere: ash particles — always active
            if (st.ambientTimer % 10 == 0) {
                Random r = clone.rand;
                for (int i = 0; i < 12; i++) {
                    double px = player.getPosX() + (r.nextDouble() - 0.5) * 40;
                    double py = player.getPosY() + r.nextDouble() * 15;
                    double pz = player.getPosZ() + (r.nextDouble() - 0.5) * 40;
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
                player.playSound(creepy[clone.rand.nextInt(creepy.length)], 0.4F, 0.3F + clone.rand.nextFloat() * 0.3F);
            }

            // Wall ambient effects — dark particles around the wall ring
            spawnWallAmbient(clone, st);
            LiminalWallParticles.tick(clone, player, st);

            // Everything below this point requires first hit
            if (!st.firstHit) continue;

            st.ticks++;

            // Withering effect — only after 2 minutes
            if (st.ticks >= 2400 && st.ticks % 60 == 0 && player.getHealth() > 1f)
                player.attackEntityFrom(net.minecraft.util.DamageSource.WITHER, 1f);

            // Slowness based on kill count
            if (st.copiesKilled >= 4 && st.ticks % (10 * 20) == 0) {
                int amplifier = st.copiesKilled >= 5 ? 2 : 0;
                int duration = st.copiesKilled >= 5 ? 6 * 20 : 4 * 20;
                player.addPotionEffect(new EffectInstance(Effects.SLOWNESS, duration, amplifier, false, false));
            }

            // Shadow respawn
            if (!st.shadowSpawned
                && st.shadowRespawnTimer > 0
                && (!st.finalRitualStarted || st.shadowLivesBroken < 4)) {
                st.shadowRespawnTimer--;
                if (st.shadowRespawnTimer == 0) {
                    st.shadowSpawned = spawnShadow(clone, st, player);
                    Entity entity = st.shadowId == null
                        ? null : clone.getEntityByUuid(st.shadowId);
                    if (entity instanceof ShadowEntity && st.firstHit) {
                        ((ShadowEntity) entity).setAggressive(true);
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
                        if (ent != null) {
                            if (ent instanceof PlayerCopyEntity
                                && ((PlayerCopyEntity) ent).isHostileCopy()) {
                                st.activeArmedCopies = Math.max(0, st.activeArmedCopies - 1);
                            }
                            ent.remove();
                        }
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
                    if (ent != null) {
                        if (ent instanceof PlayerCopyEntity
                            && ((PlayerCopyEntity) ent).isHostileCopy()) {
                            st.activeArmedCopies = Math.max(0, st.activeArmedCopies - 1);
                        }
                        ent.remove();
                    }
                    st.spawnTicks.remove(oldest);
                    st.copyIds.remove(oldest);
                    st.activeCopies = Math.max(0, st.activeCopies - 1);
                }
            }

            // Director-based copy spawning (disabled during boss fight)
            if (!st.finalRitualStarted) {
                LiminalDirector.tick(clone, player, st);
            }

            // Wall self-repair every 10 seconds
            if (st.ticks % 200 == 0) {
                repairWallSlice(clone, st);
            }

            // Second half (after 3 minutes = 3600 ticks)
            if (!st.secondHalfAnomalies && st.ticks >= 3600) {
                st.secondHalfAnomalies = true;
                sendCopyMessage(player, "\u041C\u0438\u0440 \u0440\u0443\u0448\u0438\u0442\u0441\u044F...");
            }

            // World destruction — based on phase, not time
            tickPhaseCollapse(clone, player, st);

            // Trigger rage music when shadow becomes aggressive
            if (st.shadowAggressive && !st.musicStarted) {
                switchToRageMusic(player, st);
            }

            // Prologue complete: start boss fight
            if (st.copiesKilled >= KILLS_TO_ESCAPE && !st.finalRitualStarted) {
                st.finalRitualStarted = true;
                st.shadowLivesBroken = 0;
                st.shadowPhase = ShadowPhase.AWAKENED;
                st.shadowRespawnTimer = 0;

                for (UUID id : new ArrayList<>(st.copyIds)) {
                    Entity copy = clone.getEntityByUuid(id);
                    if (copy != null) copy.remove();
                }
                st.copyIds.clear();
                st.spawnTicks.clear();
                st.activeCopies = 0;

                Entity oldShadow = st.shadowId == null
                    ? null : clone.getEntityByUuid(st.shadowId);
                if (oldShadow != null) oldShadow.remove();

                st.shadowSpawned = spawnShadow(clone, st, player);
                Entity boss = st.shadowId == null ? null : clone.getEntityByUuid(st.shadowId);
                if (boss instanceof ShadowEntity) {
                    ((ShadowEntity) boss).setAggressive(true);
                }
                sendCopyMessage(player, "\u0428\u0435\u0441\u0442\u044C \u043E\u0442\u0440\u0430\u0436\u0435\u043D\u0438\u0439 \u0440\u0430\u0437\u0431\u0438\u0442\u044B. \u0422\u0435\u043F\u0435\u0440\u044C \u043E\u0441\u0442\u0430\u043D\u0435\u0442\u0441\u044F \u0442\u043E\u043B\u044C\u043A\u043E \u043E\u0434\u043D\u043E.");
            }
        }

        for (ServerPlayerEntity p : new java.util.ArrayList<>(clone.getPlayers())) {
            State state = STATES.get(p.getUniqueID());
            if (state != null) continue;

            ServerWorld overworld = clone.getServer().getWorld(net.minecraft.world.World.OVERWORLD);
            if (overworld != null) {
                BlockPos spawn = overworld.getSpawnPoint();
                p.teleport(overworld,
                        spawn.getX() + 0.5D,
                        spawn.getY() + 1.0D,
                        spawn.getZ() + 0.5D,
                        p.rotationYaw,
                        p.rotationPitch);
                clearLockEffects(p);
            }
        }

        if (STATES.isEmpty()) {
            clearDimensionIfEmpty(clone);
        }
    }

    public static void clearDimensionIfEmpty(ServerWorld clone) {
        if (clone == null) return;
        if (!new java.util.ArrayList<>(clone.getPlayers()).isEmpty()) return;
        TitanForge.LOGGER.info("[liminal] dimension empty");
    }

    private static void tickPhaseCollapse(ServerWorld world,
                                          ServerPlayerEntity player,
                                          State state) {
        if (!state.firstHit) return;

        int interval;
        int foci;
        int radius;

        if (!state.finalRitualStarted) {
            if (state.copiesKilled <= 1) {
                interval = 80;
                foci = 1;
                radius = 2;
            } else if (state.copiesKilled <= 3) {
                interval = 60;
                foci = 2;
                radius = 2;
            } else if (state.copiesKilled <= 5) {
                interval = 45;
                foci = 3;
                radius = 3;
            } else {
                interval = 35;
                foci = 4;
                radius = 3;
            }
        } else {
            switch (state.shadowPhase) {
                case AWAKENED:
                    interval = 60;
                    foci = 2;
                    radius = 2;
                    break;
                case HUNTER:
                    interval = 45;
                    foci = 3;
                    radius = 3;
                    break;
                case FRACTURED:
                    interval = 30;
                    foci = 4;
                    radius = 3;
                    break;
                case FINAL:
                    interval = 20;
                    foci = 6;
                    radius = 4;
                    break;
                default:
                    interval = 80;
                    foci = 1;
                    radius = 2;
                    break;
            }
        }

        if (state.collapseCooldown > 0) {
            state.collapseCooldown--;
            return;
        }

        state.collapseCooldown = interval;
        collapseSurface(world, player, state, foci, radius);
    }

    private static void collapseSurface(ServerWorld world,
                                        ServerPlayerEntity player,
                                        State state,
                                        int foci,
                                        int radius) {
        for (int i = 0; i < foci; i++) {
            BlockPos center = findCollapseSurface(world, player, state);
            if (center == null) continue;

            if (world.rand.nextFloat() < 0.22F) {
                world.createExplosion(
                    null,
                    center.getX() + 0.5D,
                    center.getY() + 0.5D,
                    center.getZ() + 0.5D,
                    1.8F + radius * 0.35F,
                    false,
                    net.minecraft.world.Explosion.Mode.DESTROY);
            }

            for (BlockPos pos : BlockPos.getAllInBoxMutable(
                    center.add(-radius, -radius, -radius),
                    center.add(radius, radius, radius))) {

                if (!insideArena(state, pos, 5.0D)) continue;
                if (isProtectedWall(world, pos)) continue;
                if (world.getTileEntity(pos) != null) continue;
                if (pos.distanceSq(player.getPosition()) < 25.0D) continue;

                double distance = Math.sqrt(pos.distanceSq(center));
                if (distance > radius + 0.35D) continue;

                BlockState current = world.getBlockState(pos);
                if (current.isAir()) continue;
                if (current.getBlock() == Blocks.BEDROCK
                    || current.getBlock() == Blocks.BARRIER
                    || current.getBlock() == Blocks.BLACK_CONCRETE) continue;

                float chance = 0.26F + (float) ((radius - distance) * 0.11D);
                if (world.rand.nextFloat() > chance) continue;

                if (!canDecay(pos, current)) continue;

                if (world.getTileEntity(pos) != null) {
                    world.removeTileEntity(pos);
                }

                world.setBlockState(
                    pos,
                    ModBlocks.YELLOW_DECAY_BLOCK.get().getDefaultState(),
                    18);

                world.spawnParticle(
                    ParticleTypes.ASH,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.6D,
                    pos.getZ() + 0.5D,
                    3,
                    0.25D, 0.3D, 0.25D,
                    0.02D);
            }
        }
    }

    private static boolean canDecay(BlockPos pos,
                                    BlockState state) {
        Block block = state.getBlock();

        if (state.isAir()) return false;
        if (block == Blocks.OBSIDIAN
            || block == Blocks.CRYING_OBSIDIAN
            || block == Blocks.BEDROCK
            || block == Blocks.BARRIER
            || block == Blocks.BLACK_CONCRETE
            || block == ModBlocks.YELLOW_DECAY_BLOCK.get()
            || block == ModBlocks.RED_DECAY_BLOCK.get()) {
            return false;
        }

        return true;
    }

    private static BlockPos findCollapseSurface(ServerWorld world,
                                                ServerPlayerEntity player,
                                                State state) {
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            double distance = 10.0D + world.rand.nextDouble() * 82.0D;

            int x = state.center.getX()
                + (int) Math.round(Math.cos(angle) * distance);
            int z = state.center.getZ()
                + (int) Math.round(Math.sin(angle) * distance);

            BlockPos.Mutable cursor = new BlockPos.Mutable(
                x,
                Math.min(255, player.getPosition().getY() + 40),
                z);

            while (cursor.getY() > 1 && world.isAirBlock(cursor)) {
                cursor.move(0, -1, 0);
            }

            BlockPos found = cursor.toImmutable();
            if (world.isAirBlock(found)) continue;
            if (isProtectedWall(world, found)) continue;
            if (found.distanceSq(player.getPosition()) < 25.0D) continue;
            return found;
        }
        return null;
    }

    private static boolean insideArena(State state, BlockPos pos, double margin) {
        double dx = pos.getX() - state.center.getX();
        double dz = pos.getZ() - state.center.getZ();
        double allowed = RADIUS - margin;
        return dx * dx + dz * dz < allowed * allowed;
    }

    private static void startCalmMusic(ServerPlayerEntity player, State state) {
        if (state.shadowAggressive || state.firstHit) return;
        NetworkHandler.sendTo(player, new PlayMusicPacket("liminal_calm"));
    }

    private static void switchToRageMusic(ServerPlayerEntity player,
                                          State state) {
        if (state.musicStarted) return;
        state.musicStarted = true;

        NetworkHandler.sendTo(
            player,
            new PlayMusicPacket("danger_around_the_corner"));
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
        boolean armed = stage >= 2
            && world.rand.nextFloat() < 0.20F
            && state.activeArmedCopies < 1;
        copy.setHostile(armed);

        if (armed) {
            copy.copyEquipmentFrom(player);
            copy.setHealth(20.0F);
        }
        boolean ok = world.addEntity(copy);
        if (ok) {
            state.activeCopies++;
            state.copyIds.add(copy.getUniqueID());
            state.spawnTicks.put(copy.getUniqueID(), state.ticks);
            if (armed) state.activeArmedCopies++;
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

    public static void spawnCopyNearShadow(ServerWorld world, ShadowEntity shadow) {
        UUID ownerId = shadow.getOwnerId().orElse(null);
        if (ownerId == null) return;

        State state = STATES.get(ownerId);
        ServerPlayerEntity owner = world.getServer().getPlayerList().getPlayerByUUID(ownerId);
        if (state == null || owner == null || state.activeCopies >= 6) return;

        PlayerCopyEntity copy = com.example.titanforge.ModEntities.PLAYER_COPY.get().create(world);
        if (copy == null) return;

        double angle = world.rand.nextDouble() * Math.PI * 2.0D;
        double distance = 4.0D + world.rand.nextDouble() * 4.0D;
        copy.setLocationAndAngles(
                shadow.getPosX() + Math.cos(angle) * distance,
                shadow.getPosY(),
                shadow.getPosZ() + Math.sin(angle) * distance,
                owner.rotationYaw, owner.rotationPitch);
        copy.setOwner(ownerId);
        copy.setHostile(true);
        copy.copyEquipmentFrom(owner);
        copy.setAttackTarget(owner);

        if (world.addEntity(copy)) {
            state.activeCopies++;
            state.copyIds.add(copy.getUniqueID());
            state.spawnTicks.put(copy.getUniqueID(), state.ticks);
            world.spawnParticle(net.minecraft.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    copy.getPosX(), copy.getPosY() + 1.0D, copy.getPosZ(),
                    24, 0.35D, 0.7D, 0.35D, 0.03D);
        }
    }

    public static void spawnGhostMob(ServerWorld w, State st, ServerPlayerEntity owner) {
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

    public static void spawnArmedCopy(ServerWorld w, State st, ServerPlayerEntity owner) {
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

    public static void spawnArmedSkeleton(ServerWorld w, State st, ServerPlayerEntity owner) {
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

    public static void onCopyKilled(PlayerCopyEntity copy,
                                     ServerPlayerEntity killer) {
        UUID owner = copy.getOwnerId().orElse(null);
        if (owner == null || !owner.equals(killer.getUniqueID())) return;

        State state = STATES.get(owner);
        if (state == null) return;

        UUID copyId = copy.getUniqueID();
        boolean tracked = state.copyIds.remove(copyId);
        state.spawnTicks.remove(copyId);
        if (tracked) {
            state.activeCopies = Math.max(0, state.activeCopies - 1);
            if (copy.isHostileCopy()) {
                state.activeArmedCopies = Math.max(0, state.activeArmedCopies - 1);
            }
        }

        if (tracked && !state.finalRitualStarted) {
            state.copiesKilled = Math.min(
                KILLS_TO_ESCAPE, state.copiesKilled + 1);
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
            runBetweenTicks(vp.server, () -> {
                State st2 = STATES.get(vp.getUniqueID());
                if (st2 == null || !vp.isAlive()) return;
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
        switchToRageMusic(vp, st);
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

    private static void releaseChunkTicket(ServerWorld clone, State st) {
        if (clone == null || st == null || st.center == null) return;
        ChunkPos cp = new ChunkPos(st.center);
        clone.getChunkProvider().releaseTicket(TicketType.POST_TELEPORT, cp, 1, st.victim.hashCode());
        clone.getChunkProvider().forceChunk(cp, false);
        TitanForge.LOGGER.info("[liminal] released chunk ticket for {} at {}", st.victim, cp);
    }

    private static void cleanupRuntimeState(ServerWorld world, State state) {
        if (world == null || state == null) return;

        LiminalAnomalyManager.clear(world, state.victim);
        LiminalDialogue.clear(state.victim);

        AxisAlignedBB box = new AxisAlignedBB(
                state.center.getX() - RADIUS - 8,
                0,
                state.center.getZ() - RADIUS - 8,
                state.center.getX() + RADIUS + 9,
                256,
                state.center.getZ() + RADIUS + 9);

        for (Entity entity : world.getEntitiesWithinAABB(
                Entity.class, box,
                entity -> !(entity instanceof PlayerEntity))) {
            entity.remove();
        }

        state.copyIds.clear();
        state.spawnTicks.clear();
        state.activeCopies = 0;
        state.activeArmedCopies = 0;
        state.shadowId = null;
        state.shadowSpawned = false;
    }

    private static void queueArenaCleanup(ServerWorld liminal, State state) {
        if (state == null || state.cleanupQueued) return;
        state.cleanupQueued = true;

        cleanupRuntimeState(liminal, state);
        releaseChunkTicket(liminal, state);
        ChunkCopyManager.cancelFor(state.victim);

        LiminalArenaCleaner.start(liminal,
                state.victim,
                state.center,
                RADIUS + 4,
                () -> TitanForge.LOGGER.info(
                        "[liminal] arena cleaned for {}", state.victim));
    }

    private static void exit(ServerPlayerEntity player, State state, boolean early) {
        ServerWorld overworld = player.getServer().getWorld(net.minecraft.world.World.OVERWORLD);
        player.teleport(overworld, state.realReturnPos.getX() + 0.5, state.realReturnPos.getY(), state.realReturnPos.getZ() + 0.5, player.rotationYaw, player.rotationPitch);
        player.setGameType(net.minecraft.world.GameType.SURVIVAL);
        player.removePotionEffect(Effects.BLINDNESS);
        player.removePotionEffect(Effects.SLOWNESS);
        player.removePotionEffect(Effects.JUMP_BOOST);

        com.example.titanforge.NetworkHandler.INSTANCE.send(
                net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> player),
                new com.example.titanforge.StopMusicPacket());

        ServerWorld liminal = LiminalDimension.get(player.getServer());
        if (liminal != null) queueArenaCleanup(liminal, state);
        STATES.remove(player.getUniqueID());

        if (early) player.addPotionEffect(new EffectInstance(Effects.WITHER, 10 * 20, 0));
    }

    /** \u0422\u0435\u043B\u0435\u043F\u043E\u0440\u0442/\u0441\u043C\u0435\u043D\u0443 \u0438\u0437\u043C\u0435\u0440\u0435\u043D\u0438\u044F \u043D\u0435\u043B\u044C\u0437\u044F \u0434\u0435\u043B\u0430\u0442\u044C \u0432\u043E \u0432\u0440\u0435\u043C\u044F \u0442\u0438\u043A\u0430 \u0441\u0443\u0449\u043D\u043E\u0441\u0442\u0438
     *  ("Removing entity while ticking!") \u2014 server.execute \u043D\u0430 \u0441\u0435\u0440\u0432\u0435\u0440\u043D\u043E\u043C \u043F\u043E\u0442\u043E\u043A\u0435
     *  \u0432\u044B\u043F\u043E\u043B\u043D\u044F\u0435\u0442\u0441\u044F \u0441\u0440\u0430\u0437\u0443, \u043F\u043E\u044D\u0442\u043E\u043C\u0443 \u043E\u0442\u043A\u043B\u0430\u0434\u044B\u0432\u0430\u0435\u043C \u0432 \u043E\u0447\u0435\u0440\u0435\u0434\u044C \u043C\u0435\u0436\u0434\u0443 \u0442\u0438\u043A\u0430\u043C\u0438. */
    private static void runBetweenTicks(net.minecraft.server.MinecraftServer server, Runnable task) {
        server.enqueue(new net.minecraft.util.concurrent.TickDelayedTask(server.getTickCounter(), task));
    }

    public static void completeShadowVictory(ServerPlayerEntity player, State state) {
        runBetweenTicks(player.server, () -> {
            if (!player.isAlive()) return;
            exit(player, state, true);
            RewardShadowManager.spawnAfterVictory(player);
        });
    }

    public static void forceExit(ServerPlayerEntity player, boolean died) {
        State state = STATES.remove(player.getUniqueID());
        if (state == null) return;

        ServerWorld liminal = LiminalDimension.get(player.getServer());
        if (liminal != null) queueArenaCleanup(liminal, state);

        BlockPos returnPos = state.realReturnPos;
        runBetweenTicks(player.server, () -> {
            clearLockEffects(player);
            if (!player.isAlive()) return; // \u0443\u043C\u0435\u0440/\u0432\u044B\u0448\u0435\u043B \u2014 \u0432\u0435\u0440\u043D\u0451\u0442\u0441\u044F \u0447\u0435\u0440\u0435\u0437 \u043E\u0431\u044B\u0447\u043D\u044B\u0439 \u0440\u0435\u0441\u043F\u0430\u0432\u043D
            ServerWorld overworld = player.getServer().getWorld(net.minecraft.world.World.OVERWORLD);
            player.teleport(overworld, returnPos.getX() + 0.5, returnPos.getY(), returnPos.getZ() + 0.5, player.rotationYaw, player.rotationPitch);
            com.example.titanforge.NetworkHandler.INSTANCE.send(
                    net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> player),
                    new com.example.titanforge.StopMusicPacket());
        });

        if (died) player.sendStatusMessage(new StringTextComponent("\u00A74\u0422\u044B \u043D\u0435 \u0432\u044B\u0431\u0440\u0430\u043B\u0441\u044F..."), false);
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

    public static void onExternalDimensionExit(ServerPlayerEntity player) {
        State state = STATES.remove(player.getUniqueID());
        if (state == null) return;
        ServerWorld liminal = LiminalDimension.get(player.getServer());
        if (liminal != null) queueArenaCleanup(liminal, state);
        clearLockEffects(player);
    }

    public static void onPlayerDeath(ServerPlayerEntity player) {
        UUID id = player.getUniqueID();
        State state = STATES.remove(id);
        if (state == null) return;

        ServerWorld liminal = LiminalDimension.get(player.getServer());
        if (liminal != null) queueArenaCleanup(liminal, state);
        clearLockEffects(player);
    }

    public static void clearLockEffects(ServerPlayerEntity vp) {
        vp.removePotionEffect(Effects.BLINDNESS);
        vp.removePotionEffect(Effects.SLOWNESS);
        vp.removePotionEffect(Effects.JUMP_BOOST);
        if (vp.isSpectator() || vp.isCreative())
            vp.setGameType(net.minecraft.world.GameType.SURVIVAL);
    }

    public static void onLogout(ServerPlayerEntity player) {
        State state = STATES.remove(player.getUniqueID());
        if (state == null) return;
        ServerWorld liminal = LiminalDimension.get(player.getServer());
        if (liminal != null) queueArenaCleanup(liminal, state);
    }

    public static void onLogin(ServerPlayerEntity player) {
        State state = STATES.remove(player.getUniqueID());
        if (state == null) return;
        ServerWorld liminal = LiminalDimension.get(player.getServer());
        if (liminal != null) queueArenaCleanup(liminal, state);
        clearLockEffects(player);
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
