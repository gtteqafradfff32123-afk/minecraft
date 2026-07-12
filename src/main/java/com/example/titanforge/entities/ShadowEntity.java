package com.example.titanforge.entities;

import com.example.titanforge.entities.ai.ShadowStalkGoal;
import com.example.titanforge.liminal.LiminalManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;

public class ShadowEntity extends MobEntity {
    private static final DataParameter<Optional<UUID>> OWNER =
            EntityDataManager.createKey(ShadowEntity.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    private static final DataParameter<Boolean> IS_TARGET =
            EntityDataManager.createKey(ShadowEntity.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> AGGRESSIVE =
            EntityDataManager.createKey(ShadowEntity.class, DataSerializers.BOOLEAN);
    private GameProfile cachedProfile;
    private int spawnCopyTimer = 0;
    private int shellHits;
    private int acceptedShellHits;
    private int invulnerabilityTicks;
    private int abilityCooldown;
    private int vanishTicks;
    private int attackWindup;
    private int currentAbility;
    private BlockPos ritualCenter;

    public enum DefeatedMode {
        STAY,
        FOLLOW,
        RETURN_HOME
    }

    private boolean defeated;
    private UUID defeatedOwner;
    private BlockPos rewardHome;
    private DefeatedMode defeatedMode = DefeatedMode.STAY;

    public ShadowEntity(EntityType<? extends MobEntity> type, World world) {
        super(type, world);
    }

    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return MobEntity.func_233666_p_()
                .createMutableAttribute(Attributes.MAX_HEALTH, 100.0D)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.32D)
                .createMutableAttribute(Attributes.KNOCKBACK_RESISTANCE, 1.0D)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 128.0D)
                .createMutableAttribute(Attributes.ARMOR, 20.0D)
                .createMutableAttribute(Attributes.ARMOR_TOUGHNESS, 8.0D);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(OWNER, Optional.empty());
        this.dataManager.register(IS_TARGET, false);
        this.dataManager.register(AGGRESSIVE, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new ShadowStalkGoal(this));
        this.goalSelector.addGoal(2, new SwimGoal(this));
    }

    public void setOwner(UUID id) {
        this.cachedProfile = null;
        this.dataManager.set(OWNER, Optional.of(id));
    }

    public Optional<UUID> getOwnerId() {
        return this.dataManager.get(OWNER);
    }

    public GameProfile getOwnerProfile() {
        Optional<UUID> opt = getOwnerId();
        if (!opt.isPresent()) return null;

        UUID ownerId = opt.get();
        PlayerEntity player = this.world.getPlayerByUuid(ownerId);
        if (player != null) {
            cachedProfile = player.getGameProfile();
            return cachedProfile;
        }

        if (cachedProfile == null || !ownerId.equals(cachedProfile.getId())) {
            cachedProfile = new GameProfile(ownerId, "");
        }
        return cachedProfile;
    }

    public void makeDefeated(UUID owner, BlockPos home) {
        this.defeated = true;
        this.defeatedOwner = owner;
        this.rewardHome = home.toImmutable();
        this.defeatedMode = DefeatedMode.STAY;
        this.setAggressive(false);
        this.setTarget(false);
        this.setInvulnerable(true);
        this.getNavigator().clearPath();
    }

    public boolean isDefeated() {
        return defeated;
    }

    public UUID getDefeatedOwner() {
        return defeatedOwner;
    }

    public void setDefeatedMode(DefeatedMode mode) {
        if (!defeated) return;
        defeatedMode = mode;
        if (mode == DefeatedMode.STAY) getNavigator().clearPath();
    }

    private void tickDefeated() {
        if (!defeated || rewardHome == null) return;

        PlayerEntity owner = defeatedOwner == null
            ? null : world.getPlayerByUuid(defeatedOwner);

        if (defeatedMode == DefeatedMode.FOLLOW) {
            if (owner == null) {
                defeatedMode = DefeatedMode.RETURN_HOME;
                return;
            }
            double distance = this.getDistance(owner);
            if (distance > 5.0D) {
                getNavigator().tryMoveToEntityLiving(owner, 0.9D);
            } else if (distance < 2.8D) {
                getNavigator().clearPath();
            }
            getLookController().setLookPositionWithEntity(owner, 20F, 20F);
            return;
        }

        if (defeatedMode == DefeatedMode.RETURN_HOME) {
            double distanceSq = getDistanceSq(
                rewardHome.getX() + 0.5D,
                rewardHome.getY(),
                rewardHome.getZ() + 0.5D);
            if (distanceSq <= 2.25D) {
                getNavigator().clearPath();
                defeatedMode = DefeatedMode.STAY;
            } else {
                getNavigator().tryMoveToXYZ(
                    rewardHome.getX() + 0.5D,
                    rewardHome.getY(),
                    rewardHome.getZ() + 0.5D,
                    0.9D);
            }
            return;
        }

        getNavigator().clearPath();
        setMotion(0.0D, getMotion().y, 0.0D);
    }

    public void setTarget(boolean b) {
        this.dataManager.set(IS_TARGET, b);
        if (b) {
            this.addPotionEffect(new EffectInstance(Effects.GLOWING, Integer.MAX_VALUE, 0, false, false));
        } else {
            this.removePotionEffect(Effects.GLOWING);
        }
    }

    public boolean isTarget() {
        return this.dataManager.get(IS_TARGET);
    }

    public void setAggressive(boolean b) {
        this.dataManager.set(AGGRESSIVE, b);
    }

    public boolean isAggressive() {
        return this.dataManager.get(AGGRESSIVE);
    }

    public int getAbilityCooldown() { return abilityCooldown; }
    public void setAbilityCooldown(int value) { abilityCooldown = value; }
    public int getAttackWindup() { return attackWindup; }
    public void setAttackWindup(int value) { attackWindup = value; }
    public int getCurrentAbility() { return currentAbility; }
    public void setCurrentAbility(int value) { currentAbility = value; }

    @Override
    public void livingTick() {
        super.livingTick();
        if (world.isRemote) return;

        if (defeated) {
            tickDefeated();
            return;
        }

        if (invulnerabilityTicks > 0) invulnerabilityTicks--;
        if (abilityCooldown > 0) abilityCooldown--;
        if (vanishTicks > 0) {
            vanishTicks--;
            setInvisible(true);
            setNoAI(true);
            if (vanishTicks == 0) {
                setInvisible(false);
                setNoAI(false);
            }
        }

        if (isTarget() && ticksExisted % 40 == 0) {
            world.playSound(null, getPosition(),
                SoundEvents.ENTITY_ELDER_GUARDIAN_AMBIENT, SoundCategory.HOSTILE, 0.5F, 0.4F);
            ((ServerWorld) world).spawnParticle(ParticleTypes.SOUL,
                getPosX(), getPosY() + 1, getPosZ(), 6, 0.3, 0.5, 0.3, 0.02);
        }

        if (isAggressive()) {
            spawnCopyTimer++;
            if (spawnCopyTimer >= 300) {
                spawnCopyTimer = 0;
                LiminalManager.spawnCopyNearShadow((ServerWorld) world, this);
            }
        }

        if (!world.isRemote && isAggressive()) {
            com.example.titanforge.liminal.ShadowCombatManager.tick(this);
        }
    }

    @Override
    public boolean onLivingFall(float dist, float mult) {
        return false;
    }

    private int requiredShellHits(LiminalManager.State state) {
        switch (state.shadowPhase) {
            case AWAKENED: return 3;
            case HUNTER: return 4;
            case FRACTURED: return 5;
            case FINAL: return 6;
            default: return 1;
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.OUT_OF_WORLD) {
            return super.attackEntityFrom(source, amount);
        }
        if (defeated) return false;

        if (source.getTrueSource() instanceof AbstractArrowEntity
            || source.getTrueSource() instanceof TridentEntity) {
            if (!world.isRemote) {
                Vector3d look = getLookVec();
                setMotion(look.x * 0.4D, 0.3D, look.z * 0.4D);
                velocityChanged = true;
                ((ServerWorld) world).spawnParticle(ParticleTypes.SMOKE,
                    getPosX(), getPosY() + 1.0D, getPosZ(),
                    12, 0.3D, 0.5D, 0.3D, 0.01D);
            }
            return false;
        }

        if (!world.isRemote && source.getTrueSource() instanceof ServerPlayerEntity) {
            ServerPlayerEntity attacker = (ServerPlayerEntity) source.getTrueSource();

            if (!isAggressive()) {
                setAggressive(true);
                if (source.getTrueSource() instanceof ServerPlayerEntity) {
                    LiminalManager.onFirstHit(attacker);
                }
                return false;
            }

            if (invulnerabilityTicks > 0) return false;
            if (!(source.getTrueSource() instanceof ServerPlayerEntity)) return false;

            ServerPlayerEntity serverAttacker = (ServerPlayerEntity) source.getTrueSource();
            LiminalManager.State state = LiminalManager.getState(serverAttacker.getUniqueID());
            if (state == null) return false;

            shellHits++;
            acceptedShellHits++;
            invulnerabilityTicks = 16;
            com.example.titanforge.liminal.ShadowCombatManager.onShellHit(
                this, serverAttacker, state, acceptedShellHits % 3 == 0);

            if (shellHits < requiredShellHits(state)) return false;

            shellHits = 0;
            com.example.titanforge.liminal.ShadowCombatManager.breakShell(this, serverAttacker, state);
            return false;
        }
        return false;
    }

    @Override
    public void writeAdditional(net.minecraft.nbt.CompoundNBT nbt) {
        super.writeAdditional(nbt);
        getOwnerId().ifPresent(id -> nbt.putUniqueId("ShadowOwner", id));
        nbt.putBoolean("Aggressive", isAggressive());
        nbt.putBoolean("TitanForgeDefeated", defeated);
        if (defeatedOwner != null)
            nbt.putUniqueId("TitanForgeDefeatedOwner", defeatedOwner);
        if (rewardHome != null) {
            nbt.putInt("TitanForgeHomeX", rewardHome.getX());
            nbt.putInt("TitanForgeHomeY", rewardHome.getY());
            nbt.putInt("TitanForgeHomeZ", rewardHome.getZ());
        }
        nbt.putString("TitanForgeDefeatedMode", defeatedMode.name());
    }

    @Override
    public void readAdditional(net.minecraft.nbt.CompoundNBT nbt) {
        super.readAdditional(nbt);
        if (nbt.hasUniqueId("ShadowOwner")) setOwner(nbt.getUniqueId("ShadowOwner"));
        setAggressive(nbt.getBoolean("Aggressive"));
        defeated = nbt.getBoolean("TitanForgeDefeated");
        if (nbt.hasUniqueId("TitanForgeDefeatedOwner"))
            defeatedOwner = nbt.getUniqueId("TitanForgeDefeatedOwner");
        if (nbt.contains("TitanForgeHomeX")) {
            rewardHome = new BlockPos(
                nbt.getInt("TitanForgeHomeX"),
                nbt.getInt("TitanForgeHomeY"),
                nbt.getInt("TitanForgeHomeZ"));
        }
        try {
            defeatedMode = DefeatedMode.valueOf(
                nbt.getString("TitanForgeDefeatedMode"));
        } catch (Exception ignored) {
            defeatedMode = DefeatedMode.STAY;
        }
        if (defeated) {
            setAggressive(false);
            setInvulnerable(true);
        }
    }
}
