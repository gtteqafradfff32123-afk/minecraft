package com.example.titanforge.liminal;

import com.example.titanforge.entities.ShadowEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.List;

public final class ShadowCombatManager {
    private static final int NONE = 0;
    private static final int THREAD_LASH = 1;
    private static final int FALSE_SELF = 2;
    private static final int LIGHT_ERASURE = 3;
    private static final int ROOM_FOLD = 4;
    private static final int NAMELESS_GAZE = 5;

    private ShadowCombatManager() {}

    public static void tick(ShadowEntity shadow) {
        if (!(shadow.world instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) shadow.world;
        ServerPlayerEntity player = shadow.getOwnerId()
                .map(id -> world.getServer().getPlayerList().getPlayerByUUID(id))
                .orElse(null);
        if (player == null || !player.isAlive()) return;

        LiminalManager.State state = LiminalManager.getState(player.getUniqueID());
        if (state == null) return;

        shadow.getLookController().setLookPositionWithEntity(player, 50.0F, 50.0F);

        if (shadow.getAttackWindup() > 0) {
            telegraph(shadow, player);
            shadow.setAttackWindup(shadow.getAttackWindup() - 1);
            if (shadow.getAttackWindup() == 0) execute(shadow, player, state);
            return;
        }

        if (shadow.getAbilityCooldown() > 0) return;
        int ability = chooseAbility(shadow, player, state);
        shadow.setCurrentAbility(ability);
        shadow.setAttackWindup(windupFor(ability));
        shadow.setAbilityCooldown(cooldownFor(state));
    }

    private static int chooseAbility(ShadowEntity shadow, ServerPlayerEntity player,
                                     LiminalManager.State state) {
        double distance = shadow.getDistance(player);
        if (distance < 6.0D) return THREAD_LASH;
        if (state.shadowPhase == ShadowPhase.AWAKENED) return FALSE_SELF;
        if (state.shadowPhase == ShadowPhase.HUNTER)
            return shadow.world.rand.nextBoolean() ? FALSE_SELF : LIGHT_ERASURE;
        if (state.shadowPhase == ShadowPhase.FRACTURED)
            return 2 + shadow.world.rand.nextInt(3);
        return 1 + shadow.world.rand.nextInt(5);
    }

    private static int windupFor(int ability) {
        if (ability == THREAD_LASH) return 24;
        if (ability == ROOM_FOLD) return 50;
        return 36;
    }

    private static int cooldownFor(LiminalManager.State state) {
        return Math.max(55, 130 - state.collapseStage * 7);
    }

    private static void telegraph(ShadowEntity shadow, ServerPlayerEntity player) {
        ServerWorld world = (ServerWorld) shadow.world;
        if (shadow.ticksExisted % 3 != 0) return;
        world.spawnParticle(ParticleTypes.REVERSE_PORTAL,
                shadow.getPosX(), shadow.getPosY() + 1.0D, shadow.getPosZ(),
                5, 0.25D, 0.55D, 0.25D, 0.03D);
        if (shadow.getCurrentAbility() == THREAD_LASH) {
            Vector3d delta = player.getPositionVec().subtract(shadow.getPositionVec());
            for (int i = 1; i <= 8; i++) {
                Vector3d point = shadow.getPositionVec().add(delta.scale(i / 8.0D));
                world.spawnParticle(ParticleTypes.SMOKE,
                        point.x, point.y + 0.8D, point.z, 1, 0, 0, 0, 0);
            }
        }
    }

    private static void execute(ShadowEntity shadow, ServerPlayerEntity player,
                                LiminalManager.State state) {
        switch (shadow.getCurrentAbility()) {
            case THREAD_LASH:
                threadLash(shadow, player);
                break;
            case FALSE_SELF:
                falseSelf((ServerWorld) shadow.world, player, state);
                break;
            case LIGHT_ERASURE:
                lightErasure((ServerWorld) shadow.world, player);
                break;
            case ROOM_FOLD:
                roomFold((ServerWorld) shadow.world, player, state);
                break;
            case NAMELESS_GAZE:
                namelessGaze(shadow, player);
                break;
            default:
                break;
        }
        shadow.setCurrentAbility(NONE);
    }

    private static void threadLash(ShadowEntity shadow, ServerPlayerEntity player) {
        if (shadow.getDistanceSq(player) > 100.0D) return;
        if (!shadow.canEntityBeSeen(player)) return;
        player.attackEntityFrom(DamageSource.causeMobDamage(shadow), 4.0F);
        Vector3d pull = shadow.getPositionVec().subtract(player.getPositionVec())
                .normalize().scale(0.65D);
        player.setMotion(player.getMotion().add(pull.x, 0.18D, pull.z));
        player.velocityChanged = true;
        player.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 45, 1));
    }

    private static void falseSelf(ServerWorld world, ServerPlayerEntity player,
                                  LiminalManager.State state) {
        for (int i = 0; i < 2 + Math.min(3, state.collapseStage / 3); i++) {
            LiminalManager.spawnDirectorCopy(world, state, player, state.collapseStage);
        }
        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
                SoundCategory.HOSTILE, 0.8F, 0.55F);
    }

    private static void lightErasure(ServerWorld world, ServerPlayerEntity player) {
        BlockPos center = player.getPosition();
        for (BlockPos pos : BlockPos.getAllInBoxMutable(center.add(-7, -3, -7),
                center.add(7, 4, 7))) {
            if (world.getBlockState(pos).getLightValue(world, pos) > 0
                    && world.getTileEntity(pos) == null
                    && !LiminalManager.isProtectedWall(world, pos)) {
                world.setBlockState(pos, net.minecraft.block.Blocks.AIR.getDefaultState(), 3);
            }
        }
        player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 55, 0));
    }

    private static void roomFold(ServerWorld world, ServerPlayerEntity player,
                                 LiminalManager.State state) {
        Vector3d look = player.getLookVec().mul(1.0D, 0.0D, 1.0D).normalize();
        double x = player.getPosX() - look.x * 12.0D;
        double z = player.getPosZ() - look.z * 12.0D;
        if (inside(state, x, z)) {
            player.setPositionAndUpdate(x, player.getPosY(), z);
            player.addPotionEffect(new EffectInstance(Effects.NAUSEA, 80, 0));
        }
    }

    private static void namelessGaze(ShadowEntity shadow, ServerPlayerEntity player) {
        Vector3d toShadow = shadow.getPositionVec().subtract(player.getPositionVec()).normalize();
        if (player.getLookVec().normalize().dotProduct(toShadow) < 0.82D) return;
        player.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 100, 1));
        player.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, 100, 1));
        player.getFoodStats().setFoodLevel(Math.max(0, player.getFoodStats().getFoodLevel() - 2));
    }

    public static void onShellHit(ShadowEntity shadow, ServerPlayerEntity player,
                                  LiminalManager.State state, boolean teleport) {
        ServerWorld world = (ServerWorld) shadow.world;
        world.spawnParticle(ParticleTypes.DRAGON_BREATH,
                shadow.getPosX(), shadow.getPosY() + 1.0D, shadow.getPosZ(),
                30, 0.4D, 0.8D, 0.4D, 0.1D);
        world.playSound(null, shadow.getPosition(), SoundEvents.ENTITY_WITHER_HURT,
                SoundCategory.HOSTILE, 0.8F, 0.45F);

        if (teleport) teleportAroundPlayer(shadow, player, state);
    }

    public static void breakShell(ShadowEntity shadow, ServerPlayerEntity player,
                                  LiminalManager.State state) {
        ServerWorld world = (ServerWorld) shadow.world;
        world.spawnParticle(ParticleTypes.EXPLOSION,
                shadow.getPosX(), shadow.getPosY() + 1.0D, shadow.getPosZ(),
                5, 0.5D, 0.8D, 0.5D, 0.0D);

        if (state.shadowPhase == ShadowPhase.FINAL) {
            state.shadowLivesBroken = 4;
            state.shadowSpawned = false;
            shadow.remove();
            LiminalManager.completeShadowVictory(player, state);
            return;
        }

        int adds;
        switch (state.shadowPhase) {
            case AWAKENED: adds = 1; break;
            case HUNTER: adds = 2; break;
            case FRACTURED: adds = 2; break;
            default: adds = 0;
        }
        for (int i = 0; i < adds; i++) {
            LiminalManager.spawnDirectorCopy(world, state, player, state.collapseStage);
        }

        state.shadowLivesBroken++;
        advancePhase(state);
        state.shadowSpawned = false;
        state.shadowRespawnTimer = 60;
        shadow.remove();
    }

    private static void advancePhase(LiminalManager.State state) {
        switch (state.shadowLivesBroken) {
            case 0: state.shadowPhase = ShadowPhase.AWAKENED; break;
            case 1: state.shadowPhase = ShadowPhase.HUNTER; break;
            case 2: state.shadowPhase = ShadowPhase.FRACTURED; break;
            default: state.shadowPhase = ShadowPhase.FINAL; break;
        }
    }

    private static void teleportAroundPlayer(ShadowEntity shadow, ServerPlayerEntity player,
                                             LiminalManager.State state) {
        ServerWorld world = (ServerWorld) shadow.world;
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            double distance = 9.0D + world.rand.nextDouble() * 15.0D;
            double x = player.getPosX() + Math.cos(angle) * distance;
            double z = player.getPosZ() + Math.sin(angle) * distance;
            if (!inside(state, x, z)) continue;
            BlockPos pos = new BlockPos(x, player.getPosY(), z);
            if (world.isAirBlock(pos) && world.isAirBlock(pos.up())
                    && !world.isAirBlock(pos.down())) {
                shadow.setPositionAndUpdate(x, pos.getY(), z);
                return;
            }
        }
    }

    private static boolean inside(LiminalManager.State state, double x, double z) {
        double dx = x - state.center.getX();
        double dz = z - state.center.getZ();
        return dx * dx + dz * dz < 92.0D * 92.0D;
    }
}
