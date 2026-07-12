package com.example.titanforge.entities;

import com.example.titanforge.entities.ai.PlayerCopyStalkGoal;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import com.mojang.authlib.GameProfile;
import java.util.Optional;
import java.util.UUID;

public class PlayerCopyEntity extends CreatureEntity {
    private static final DataParameter<Optional<UUID>> OWNER =
            EntityDataManager.createKey(PlayerCopyEntity.class, DataSerializers.OPTIONAL_UNIQUE_ID);
    private static final DataParameter<Boolean> HOSTILE =
            EntityDataManager.createKey(PlayerCopyEntity.class, DataSerializers.BOOLEAN);
    private GameProfile cachedProfile;

    public PlayerCopyEntity(EntityType<? extends PlayerCopyEntity> type, World world) {
        super(type, world);
    }

    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return MobEntity.func_233666_p_()
                .createMutableAttribute(Attributes.MAX_HEALTH, 40.0D)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.35D)
                .createMutableAttribute(Attributes.KNOCKBACK_RESISTANCE, 0.8D)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 128.0D)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 6.0D)
                .createMutableAttribute(Attributes.ARMOR, 4.0D);
    }

    @Override
    protected void registerData() {
        super.registerData();
        this.dataManager.register(OWNER, Optional.empty());
        this.dataManager.register(HOSTILE, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new MeleeAttackGoal(this, 1.15D, true) {
            @Override public boolean shouldExecute() {
                return isHostileCopy() && super.shouldExecute();
            }
        });
        this.targetSelector.addGoal(0,
                new NearestAttackableTargetGoal<>(
                        this, PlayerEntity.class, 10, true, false,
                        p -> isHostileCopy() && getOwnerId().map(id -> id.equals(p.getUniqueID())).orElse(false)));
        this.goalSelector.addGoal(1, new PlayerCopyStalkGoal(this));
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

    public String getCopyName() {
        GameProfile gp = getOwnerProfile();
        if (gp != null && gp.getName() != null && !gp.getName().isEmpty())
            return gp.getName();
        return "Copy";
    }

    public void setHostile(boolean hostile) {
        this.dataManager.set(HOSTILE, hostile);
    }

    public boolean isHostileCopy() {
        return this.dataManager.get(HOSTILE);
    }

    public void copyEquipmentFrom(ServerPlayerEntity player) {
        setItemStackToSlot(net.minecraft.inventory.EquipmentSlotType.MAINHAND,
                player.getHeldItemMainhand().copy());
        setItemStackToSlot(net.minecraft.inventory.EquipmentSlotType.OFFHAND,
                player.getHeldItemOffhand().copy());
        setItemStackToSlot(net.minecraft.inventory.EquipmentSlotType.HEAD,
                player.getItemStackFromSlot(net.minecraft.inventory.EquipmentSlotType.HEAD).copy());
        setItemStackToSlot(net.minecraft.inventory.EquipmentSlotType.CHEST,
                player.getItemStackFromSlot(net.minecraft.inventory.EquipmentSlotType.CHEST).copy());
        setItemStackToSlot(net.minecraft.inventory.EquipmentSlotType.LEGS,
                player.getItemStackFromSlot(net.minecraft.inventory.EquipmentSlotType.LEGS).copy());
        setItemStackToSlot(net.minecraft.inventory.EquipmentSlotType.FEET,
                player.getItemStackFromSlot(net.minecraft.inventory.EquipmentSlotType.FEET).copy());

        for (net.minecraft.inventory.EquipmentSlotType slot :
                net.minecraft.inventory.EquipmentSlotType.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    @Override
    public boolean canDropLoot() {
        return false;
    }

    @Override
    public void writeAdditional(net.minecraft.nbt.CompoundNBT nbt) {
        super.writeAdditional(nbt);
        getOwnerId().ifPresent(id -> nbt.putUniqueId("CopyOwner", id));
        nbt.putBoolean("HostileCopy", isHostileCopy());
    }

    @Override
    public void readAdditional(net.minecraft.nbt.CompoundNBT nbt) {
        super.readAdditional(nbt);
        if (nbt.hasUniqueId("CopyOwner")) setOwner(nbt.getUniqueId("CopyOwner"));
        setHostile(nbt.getBoolean("HostileCopy"));
    }

    @Override
    public void livingTick() {
        super.livingTick();
        if (world.isRemote) return;

        PlayerEntity p = getOwnerId().map(id -> world.getPlayerByUuid(id)).orElse(null);
        if (p != null) {
            float yawDelta = Math.abs(p.rotationYaw - p.prevRotationYaw);
            if (yawDelta > 40 && getDistance(p) < 15 && PlayerCopyStalkGoal.isSeenByPlayer(p, this)) {
                Vector3d away = new Vector3d(getPosX()-p.getPosX(), 0, getPosZ()-p.getPosZ()).normalize();
                setMotion(away.x * 0.4, 0, away.z * 0.4);
            }
        }
    }

    @Override
    public boolean onLivingFall(float dist, float mult) {
        return false;
    }

    @Override
    public boolean attackEntityFrom(DamageSource source, float amount) {
        if (source == DamageSource.OUT_OF_WORLD) return super.attackEntityFrom(source, amount);
        if (!isHostileCopy()) return false;

        if (source.getTrueSource() instanceof PlayerEntity) {
            PlayerEntity attacker = (PlayerEntity) source.getTrueSource();
            boolean owner = getOwnerId().map(id -> id.equals(attacker.getUniqueID())).orElse(false);
            if (!owner) return false;
        }
        return super.attackEntityFrom(source, amount);
    }
}
