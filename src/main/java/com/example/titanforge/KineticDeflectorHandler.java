package com.example.titanforge;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class KineticDeflectorHandler {
    private static final Set<UUID> REFLECTING = new HashSet<>();

    @SubscribeEvent
    public void onAttack(LivingAttackEvent event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) return;
        PlayerEntity defender = (PlayerEntity) event.getEntityLiving();
        if (defender.world.isRemote || !defender.isActiveItemStackBlocking()) return;

        ItemStack active = defender.getActiveItemStack();
        if (active.isEmpty() || active.getItem() != Items.SHIELD) return;
        int level = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.KINETIC_DEFLECTOR.get(), active);
        if (level <= 0 || REFLECTING.contains(defender.getUniqueID())) return;

        Entity trueSource = event.getSource().getTrueSource();
        if (!(trueSource instanceof LivingEntity) || trueSource == defender) return;
        LivingEntity attacker = (LivingEntity) trueSource;
        if (!isInBlockingArc(defender, event.getSource())) return;

        float reflected = Math.min(8.0F, Math.max(1.0F, event.getAmount() * 1.5F));
        attacker.hurtResistantTime = 0;
        boolean hit = attacker.attackEntityFrom(
                DamageSource.causePlayerDamage(defender).setDamageBypassesArmor(), reflected);
        if (!hit) return;

        int durabilityCost = Math.min(12, 3 + (int) Math.ceil(event.getAmount() * 0.75F));
        active.damageItem(durabilityCost, defender,
                p -> p.sendBreakAnimation(defender.getActiveHand()));

        ServerWorld world = (ServerWorld) defender.world;
        world.playSound(null, defender.getPosition(), SoundEvents.BLOCK_ANVIL_PLACE,
                SoundCategory.PLAYERS, 0.75F, 1.45F);
        world.spawnParticle(ParticleTypes.CRIT,
                attacker.getPosX(), attacker.getPosY() + attacker.getHeight() * 0.6D, attacker.getPosZ(),
                20, 0.3D, 0.4D, 0.3D, 0.08D);
    }

    private boolean isInBlockingArc(PlayerEntity defender, DamageSource source) {
        Vector3d sourcePos = source.getDamageLocation();
        if (sourcePos == null) return true;
        Vector3d flatLook = defender.getLook(1.0F).mul(1.0D, 0.0D, 1.0D).normalize();
        Vector3d fromSource = defender.getPositionVec().subtract(sourcePos)
                .mul(1.0D, 0.0D, 1.0D).normalize();
        return fromSource.dotProduct(flatLook) < 0.0D;
    }
}
