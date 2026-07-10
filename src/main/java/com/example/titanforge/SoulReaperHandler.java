package com.example.titanforge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

/**
 * Soul Reaper: убитый моб может воскреснуть как полупрозрачный призрак того же типа.
 * Единый NBT-тег "IsSoulCopy" используется и здесь, и в mixin, и в render event.
 */
@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID)
public class SoulReaperHandler {

    public static final String SOUL_COPY = "IsSoulCopy";
    public static final String SOUL_LIFE = "SoulCopyLife";
    public static final String SOUL_OWNER = "SoulOwner";

    private static final float SPAWN_CHANCE = 0.85F;
    private static final int   LIFETIME_TICKS = 600;
    private static final float MIN_HEALTH = 10.0F;
    private static final double FOLLOW_DIST_SQ = 400.0D;
    private static final double SEARCH_RANGE = 20.0D;

    @SubscribeEvent
    public static void onKill(LivingDeathEvent event) {
        LivingEntity dead = event.getEntityLiving();

        if (isSoulCopy(dead)) {
            if (!dead.world.isRemote) {
                ((ServerWorld) dead.world).spawnParticle(ParticleTypes.SOUL,
                    dead.getPosX(), dead.getPosY() + 1.0, dead.getPosZ(), 30, 0.6, 0.8, 0.6, 0.15);
            }
            return;
        }

        DamageSource source = event.getSource();
        if (!(source.getTrueSource() instanceof PlayerEntity)) return;
        if (dead.world.isRemote) return;

        PlayerEntity player = (PlayerEntity) source.getTrueSource();
        ItemStack weapon = player.getHeldItemMainhand();

        int reaperLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SOUL_REAPER.get(), weapon);
        if (reaperLvl <= 0) return;
        if (player.world.rand.nextFloat() >= SPAWN_CHANCE) return;

        Entity raw = dead.getType().create(player.world);
        if (!(raw instanceof MobEntity)) return;
        MobEntity ghost = (MobEntity) raw;

        ghost.setLocationAndAngles(dead.getPosX(), dead.getPosY(), dead.getPosZ(),
            dead.rotationYaw, dead.rotationPitch);

        CompoundNBT data = ghost.getPersistentData();
        data.putBoolean(SOUL_COPY, true);
        data.putInt(SOUL_LIFE, LIFETIME_TICKS);
        data.putUniqueId(SOUL_OWNER, player.getUniqueID());

        ghost.setHealth(Math.max(MIN_HEALTH, ghost.getMaxHealth()));

        ghost.setCustomName(new StringTextComponent("\u00A7d\u00A7o\u2620 Soul Copy"));
        ghost.setCustomNameVisible(true);
        ghost.enablePersistence();
        ghost.setSilent(true);
        ghost.setNoAI(false);
        ghost.setGlowing(true);
        ghost.addPotionEffect(new EffectInstance(Effects.FIRE_RESISTANCE, LIFETIME_TICKS, 0, false, false));
        ghost.addPotionEffect(new EffectInstance(Effects.GLOWING, LIFETIME_TICKS, 0, false, false));

        ghost.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(ghost, LivingEntity.class, 10, true, false,
            target -> {
                if (target == player || !target.isAlive()) return false;
                if (target.getPersistentData().getBoolean("ChaosDevourThrall")) return false;
                if (target.getPersistentData().getBoolean("IsSoulCopy")) return false;
                if (target instanceof MonsterEntity) return true;
                if (target instanceof MobEntity) {
                    return ((MobEntity) target).getAttackTarget() == player
                        || target.getRevengeTarget() == player;
                }
                return false;
            }));

        player.world.addEntity(ghost);

        if (!player.world.isRemote) {
            NetworkHandler.INSTANCE.send(
                net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity) player),
                new ScreenEffectPacket(2, 10));
        }

        ServerWorld sw = (ServerWorld) player.world;
        sw.spawnParticle(ParticleTypes.SOUL_FIRE_FLAME,
            dead.getPosX(), dead.getPosY() + 1.0, dead.getPosZ(), 20, 0.5, 1.0, 0.5, 0.1);
        player.world.playSound(null, dead.getPosX(), dead.getPosY(), dead.getPosZ(),
            SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 1.0F, 1.0F);
    }

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntityLiving();
        if (!isSoulCopy(victim)) return;

        Entity src = event.getSource().getTrueSource();
        if (src instanceof PlayerEntity) {
            UUID ownerId = getOwner(victim);
            if (ownerId != null && ((PlayerEntity) src).getUniqueID().equals(ownerId)) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onSoulTick(LivingEvent.LivingUpdateEvent event) {
        LivingEntity entity = event.getEntityLiving();
        if (!isSoulCopy(entity) || entity.world.isRemote) return;
        if (!(entity instanceof MobEntity)) return;

        CompoundNBT data = entity.getPersistentData();
        if (ChaosDevourHandler.isThrall(entity)) {
            data.remove(SOUL_COPY);
            data.remove(SOUL_LIFE);
            return;
        }

        MobEntity ghost = (MobEntity) entity;

        int life = data.getInt(SOUL_LIFE) - 1;
        data.putInt(SOUL_LIFE, life);
        if (entity.ticksExisted % 5 == 0) {
            ((ServerWorld) entity.world).spawnParticle(ParticleTypes.SOUL,
                entity.getPosX(), entity.getPosY() + 1.0, entity.getPosZ(), 3, 0.25, 0.35, 0.25, 0.02);
            ((ServerWorld) entity.world).spawnParticle(ParticleTypes.REVERSE_PORTAL,
                entity.getPosX(), entity.getPosY() + 0.5, entity.getPosZ(), 2, 0.2, 0.3, 0.2, 0.02);
        }
        if (life <= 0) {
            ((ServerWorld) entity.world).spawnParticle(ParticleTypes.SOUL,
                entity.getPosX(), entity.getPosY() + 1.0, entity.getPosZ(), 30, 0.5, 1.0, 0.5, 0.2);
            entity.remove();
            return;
        }

        if (!data.hasUniqueId(SOUL_OWNER)) return;
        PlayerEntity owner = entity.world.getPlayerByUuid(data.getUniqueId(SOUL_OWNER));
        if (owner == null || !owner.isAlive()) {
            ghost.remove();
            return;
        }

        if (ghost.getAttackTarget() == null || !ghost.getAttackTarget().isAlive()) {
            LivingEntity best = selectTarget(ghost, owner);
            if (best != null) {
                ghost.setAttackTarget(best);
            } else {
                if (ghost.getDistanceSq(owner) > FOLLOW_DIST_SQ) {
                    ghost.getNavigator().tryMoveToEntityLiving(owner, 1.25D);
                }
            }
        } else {
            LivingEntity target = ghost.getAttackTarget();
            double distSq = ghost.getDistanceSq(target);
            if (distSq < 4.0D) {
                ghost.getLookController().setLookPositionWithEntity(target, 90.0F, 90.0F);
                if (entity.ticksExisted % 10 == 0) {
                    target.attackEntityFrom(DamageSource.causeMobDamage(ghost), 3.0F);
                }
            } else if (distSq < FOLLOW_DIST_SQ) {
                ghost.getNavigator().tryMoveToEntityLiving(target, 1.35D);
            } else {
                ghost.setAttackTarget(null);
            }
        }
    }

    @SubscribeEvent
    public static void onDrops(LivingDropsEvent event) {
        if (isSoulCopy(event.getEntityLiving())) {
            event.getDrops().clear();
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onExpDrop(LivingExperienceDropEvent event) {
        if (isSoulCopy(event.getEntityLiving())) {
            event.setDroppedExperience(0);
        }
    }

    public static boolean isSoulCopy(LivingEntity e) {
        return e != null && e.getPersistentData().getBoolean(SOUL_COPY);
    }

    private static UUID getOwner(LivingEntity ghost) {
        CompoundNBT data = ghost.getPersistentData();
        return data.hasUniqueId(SOUL_OWNER) ? data.getUniqueId(SOUL_OWNER) : null;
    }

    private static LivingEntity selectTarget(MobEntity ghost, PlayerEntity owner) {
        AxisAlignedBB box = new AxisAlignedBB(
            ghost.getPosX() - SEARCH_RANGE, ghost.getPosY() - SEARCH_RANGE, ghost.getPosZ() - SEARCH_RANGE,
            ghost.getPosX() + SEARCH_RANGE, ghost.getPosY() + SEARCH_RANGE, ghost.getPosZ() + SEARCH_RANGE);

        List<LivingEntity> list = ghost.world.getEntitiesWithinAABB(LivingEntity.class, box);
        LivingEntity best = null;
        double bestScore = -1.0;

        for (LivingEntity e : list) {
            if (e == ghost || e == owner || !e.isAlive()) continue;
            if (isSoulCopy(e)) continue;
            if (e instanceof PlayerEntity) continue;
            if (!(e instanceof MonsterEntity) && !(e instanceof MobEntity && ((MobEntity)e).getAttackTarget() == owner)) continue;
            double distSq = ghost.getDistanceSq(e);
            if (distSq > 640.0D) continue;

            double score = Math.max(0, 20.0 - Math.sqrt(distSq));
            if (e instanceof MonsterEntity) score += 8.0;
            if (e instanceof MobEntity && ((MobEntity) e).getAttackTarget() == owner) score += 10.0;

            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }
}
