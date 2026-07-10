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
    private int invulnerabilityTicks;
    private int abilityCooldown;
    private int vanishTicks;
    private int attackWindup;
    private int currentAbility;
    private BlockPos ritualCenter;

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

    public void setTarget(boolean b) {
        this.dataManager.set(IS_TARGET, b);
        if (b) {
            this.addPotionEffect(new EffectInstance(Effects.GLOWING, Integer.MAX_VALUE, 0, false, false));
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
            case HUNTER: return 5;
            case FRACTURED: return 7;
            case FINAL: return 9;
            default: return 1;
        }
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.OUT_OF_WORLD) {
            return super.attackEntityFrom(source, amount);
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
            invulnerabilityTicks = 24;
            com.example.titanforge.liminal.ShadowCombatManager.onShellHit(this, serverAttacker, state);

            if (shellHits < requiredShellHits(state)) return false;

            shellHits = 0;
            state.shadowLivesBroken++;
            com.example.titanforge.liminal.ShadowCombatManager.breakShell(this, serverAttacker, state);
            return false;
        }
        return false;
    }
}
