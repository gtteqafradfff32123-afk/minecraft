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

    @Override
    public void livingTick() {
        super.livingTick();
        if (world.isRemote) return;

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
    }

    @Override
    public boolean onLivingFall(float dist, float mult) {
        return false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.OUT_OF_WORLD) {
            return super.attackEntityFrom(source, amount);
        }
        if (!world.isRemote && source.getTrueSource() instanceof ServerPlayerEntity) {
            ServerPlayerEntity attacker = (ServerPlayerEntity) source.getTrueSource();

            // Immune to arrows — teleport closer instead
            if (source.getImmediateSource() instanceof AbstractArrowEntity || source.getImmediateSource() instanceof TridentEntity) {
                world.playSound(null, getPosition(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                    SoundCategory.HOSTILE, 0.8F, 0.6F);
                ((ServerWorld) world).spawnParticle(ParticleTypes.PORTAL,
                    getPosX(), getPosY() + 1, getPosZ(), 40, 0.4, 0.8, 0.4, 0.3);
                // Move 5-10 blocks closer to attacker
                Vector3d dir = attacker.getPositionVec().subtract(getPositionVec()).normalize();
                double newX = getPosX() + dir.x * (5 + world.rand.nextInt(6));
                double newZ = getPosZ() + dir.z * (5 + world.rand.nextInt(6));
                BlockPos ground = new BlockPos(newX, getPosY(), newZ);
                while (ground.getY() > 0 && world.isAirBlock(ground)) ground = ground.down();
                setLocationAndAngles(newX, ground.getY() + 1, newZ, rotationYaw, rotationPitch);
                return false;
            }

            // First hit detection
            if (!isAggressive()) {
                setAggressive(true);
                LiminalManager.onFirstHit(attacker);
            }

            world.playSound(null, getPosition(), SoundEvents.ENTITY_WITHER_HURT,
                SoundCategory.HOSTILE, 0.8F, 0.5F);
            ((ServerWorld) world).spawnParticle(ParticleTypes.PORTAL,
                getPosX(), getPosY() + 1, getPosZ(), 40, 0.4, 0.8, 0.4, 0.3);

            LiminalManager.onShadowKilled(attacker);
            this.remove();
            return true;
        }
        return false;
    }
}
