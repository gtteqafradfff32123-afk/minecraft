package com.example.titanforge;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class UnstableEdgeHandler {
    private static final String GUARD = "TF_UnstableEdgeGuard";

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof PlayerEntity)) return;
        if (event.getSource().isExplosion() || event.getSource().isMagicDamage()) return;

        PlayerEntity player = (PlayerEntity) event.getSource().getTrueSource();
        if (player.world.isRemote) return;
        if (player.getPersistentData().getBoolean(GUARD)) return;

        ItemStack weapon = player.getHeldItemMainhand();
        int level = EnchantmentHelper.getEnchantmentLevel(
                ModEnchantments.UNSTABLE_EDGE.get(), weapon);
        if (level <= 0) return;

        boolean realCrit = player.fallDistance > 0.0F
                && !player.isOnGround()
                && !player.isInWater()
                && !player.isOnLadder()
                && !player.isPotionActive(net.minecraft.potion.Effects.BLINDNESS)
                && !player.isPassenger()
                && !player.isSprinting();

        // Нестабильное лезвие: 30% шанс критического урона даже без прыжка
        boolean luckyCrit = !realCrit && player.getRNG().nextFloat() < 0.30F;
        boolean crit = realCrit || luckyCrit;

        float multiplier = rollMultiplier(level, crit, player.getRNG());

        player.getPersistentData().putBoolean(GUARD, true);
        try {
            event.setAmount(event.getAmount() * multiplier);
        } finally {
            player.getPersistentData().remove(GUARD);
        }

        if (multiplier >= (crit ? 1.55F : 1.35F)) {
            LivingEntity target = event.getEntityLiving();
            ((ServerWorld) player.world).spawnParticle(
                    crit ? ParticleTypes.ENCHANTED_HIT : ParticleTypes.CRIT,
                    target.getPosX(), target.getPosY() + target.getHeight() * 0.6D,
                    target.getPosZ(), 18, 0.35D, 0.45D, 0.35D, 0.12D);
        }
    }

    private static final float[] NORMAL_MIN = {0, 1.10F, 1.15F, 1.25F};
    private static final float[] NORMAL_MAX = {0, 1.25F, 1.40F, 1.60F};
    private static final float[] CRIT_MIN   = {0, 1.25F, 1.40F, 1.60F};
    private static final float[] CRIT_MAX   = {0, 1.45F, 1.75F, 2.10F};

    /** Множитель урона; уровни выше 3 продолжают расти (+0.15 за уровень). */
    static float rollMultiplier(int level, boolean crit, java.util.Random rng) {
        int idx = Math.min(level, 3);
        float extra = level > 3 ? (level - 3) * 0.15F : 0.0F;
        float min = (crit ? CRIT_MIN : NORMAL_MIN)[idx] + extra;
        float max = (crit ? CRIT_MAX : NORMAL_MAX)[idx] + extra;
        return min + rng.nextFloat() * (max - min);
    }
}
