package com.example.titanforge.entities;

import com.example.titanforge.ModEffects;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public final class PlagueDoctorEntity extends MonsterEntity {

    public PlagueDoctorEntity(EntityType<? extends PlagueDoctorEntity> type, World world) {
        super(type, world);
        this.experienceValue = 12;
    }

    public static AttributeModifierMap.MutableAttribute createAttributes() {
        return MonsterEntity.func_234295_eP_()
                .createMutableAttribute(Attributes.MAX_HEALTH, 32.0D)
                .createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.27D)
                .createMutableAttribute(Attributes.ATTACK_DAMAGE, 5.0D)
                .createMutableAttribute(Attributes.ARMOR, 4.0D)
                .createMutableAttribute(Attributes.FOLLOW_RANGE, 28.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.05D, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomWalkingGoal(this, 0.85D));
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(7, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
    }

    @Override
    public boolean attackEntityAsMob(net.minecraft.entity.Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            if (!living.isPotionActive(ModEffects.PLAGUE.get())) {
                living.addPotionEffect(new EffectInstance(ModEffects.PLAGUE.get(), 4800, 0, false, true, true));
            }
        }
        return hit;
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
