package com.example.titanforge.entities;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.world.World;

import java.util.UUID;

public class StunZombieEntity extends ZombieEntity {
    private UUID victimId;

    public StunZombieEntity(EntityType<? extends ZombieEntity> type, World world) {
        super(type, world);
    }

    public void setVictim(UUID id) {
        this.victimId = id;
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = super.attackEntityAsMob(target);
        if (hit && target instanceof LivingEntity) {
            ((LivingEntity) target).addPotionEffect(new EffectInstance(Effects.SLOWNESS, 40, 4));
            ((LivingEntity) target).addPotionEffect(new EffectInstance(Effects.BLINDNESS, 40, 0));
        }
        return hit;
    }

    @Override
    public void livingTick() {
        super.livingTick();
        if (!world.isRemote && victimId != null) {
            PlayerEntity p = world.getPlayerByUuid(victimId);
            if (p != null) {
                this.setAttackTarget(p);
                if (this.getNavigator().getPath() == null || this.getNavigator().getPath().isFinished()) {
                    this.getNavigator().tryMoveToEntityLiving(p, 1.0D);
                }
            }
        }
    }
}
