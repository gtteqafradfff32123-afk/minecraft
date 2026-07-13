package com.example.titanforge;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Даёт NPC из CustomNPCs возможность бить зачарками с предмета в руке:
 * ванильные (острота/кара/бич, аспект огня, отдача) + зачарки TitanForge.
 */
public final class NpcEnchantHandler {
    private static final String GUARD = "TF_NpcEnchGuard";

    @SubscribeEvent
    public void onNpcAttack(LivingHurtEvent event) {
        try {
            Entity trueSrc = event.getSource().getTrueSource();
            if (!(trueSrc instanceof LivingEntity)) return;
            LivingEntity npc = (LivingEntity) trueSrc;
            if (npc instanceof PlayerEntity) return;
            if (!CustomNpcChaosBridge.isCustomNpc(npc)) return;
            if (npc.world.isRemote) return;
            if (event.getSource().isExplosion() || event.getSource().isMagicDamage()) return;
            if (npc.getPersistentData().getBoolean(GUARD)) return;

            ItemStack weapon = npc.getHeldItemMainhand();
            if (weapon.isEmpty()) return;

            LivingEntity target = event.getEntityLiving();
            ServerWorld sw = (ServerWorld) npc.world;
            float baseAmount = event.getAmount();
            float bonus = 0.0F;
            float dmgMult = 1.0F;
            long now = npc.world.getGameTime();
            CompoundNBT data = npc.getPersistentData();

            data.putBoolean(GUARD, true);
            try {
                // --- Ванильные зачарки оружия ---
                bonus += EnchantmentHelper.getModifierForCreature(weapon, target.getCreatureAttribute());

                int fireLvl = EnchantmentHelper.getEnchantmentLevel(Enchantments.FIRE_ASPECT, weapon);
                if (fireLvl > 0) target.setFire(fireLvl * 4);

                int kbLvl = EnchantmentHelper.getEnchantmentLevel(Enchantments.KNOCKBACK, weapon);
                if (kbLvl > 0) {
                    target.applyKnockback(kbLvl * 0.5F,
                            npc.getPosX() - target.getPosX(), npc.getPosZ() - target.getPosZ());
                }

                // --- Titan's Wrath ---
                int titansWrathLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.TITANS_WRATH.get(), weapon);
                if (titansWrathLvl > 0 && now > data.getLong("TF_TitanWrathCD") && npc.getRNG().nextFloat() < 0.25F) {
                    data.putLong("TF_TitanWrathCD", now + 220L);
                    float splashDamage = 3.0F + titansWrathLvl * 1.5F;
                    for (LivingEntity e : sw.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(target.getPosition()).grow(3.0))) {
                        if (e != npc && !CustomNpcChaosBridge.isCustomNpc(e)) {
                            e.hurtResistantTime = 0;
                            e.attackEntityFrom(DamageSource.causeMobDamage(npc), splashDamage);
                        }
                    }
                    bonus += 2.0F * titansWrathLvl;
                    sw.playSound(null, target.getPosX(), target.getPosY(), target.getPosZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.8F, 1.0F);
                    sw.spawnParticle(ParticleTypes.EXPLOSION, target.getPosX(), target.getPosY() + 0.5, target.getPosZ(), 4, 0.3, 0.3, 0.3, 0.02);
                }

                // --- Vampirism ---
                int vampLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.VAMPIRISM.get(), weapon);
                if (vampLvl > 0) {
                    npc.heal(baseAmount * 0.1F * vampLvl);
                    sw.spawnParticle(ParticleTypes.HEART, npc.getPosX(), npc.getPosY() + 1.0, npc.getPosZ(), 5, 0.5, 0.5, 0.5, 0.1);
                }

                // --- Chain Lightning ---
                int chainLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CHAIN_LIGHTNING.get(), weapon);
                if (chainLvl > 0 && now >= data.getLong("TF_ChainLightningCD")) {
                    data.putLong("TF_ChainLightningCD", now + 40L);
                    spawnLightning(sw, target);
                    int jumps = 0;
                    for (LivingEntity e : sw.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(target.getPosition()).grow(5))) {
                        if (e != target && e != npc && !CustomNpcChaosBridge.isCustomNpc(e)) {
                            spawnLightning(sw, e);
                            e.attackEntityFrom(DamageSource.LIGHTNING_BOLT, baseAmount * 0.5F);
                            if (++jumps >= 3) break;
                        }
                    }
                }

                // --- Void Curse ---
                int curseLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.VOID_CURSE.get(), weapon);
                if (curseLvl > 0 && now >= data.getLong("TF_VoidCurseCD")) {
                    data.putLong("TF_VoidCurseCD", now + 400L);
                    target.addPotionEffect(new EffectInstance(Effects.LEVITATION, 60, 1));
                    sw.spawnParticle(ParticleTypes.REVERSE_PORTAL, target.getPosX(), target.getPosY() + 0.8, target.getPosZ(), 24, 0.45, 0.65, 0.45, 0.06);
                }

                // --- Executioner ---
                int execLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.EXECUTIONER.get(), weapon);
                if (execLvl > 0 && target.getHealth() <= target.getMaxHealth() * 0.25F && !(target instanceof PlayerEntity)) {
                    dmgMult *= (2.0F + execLvl);
                    sw.spawnParticle(ParticleTypes.CRIT, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 30, 0.5, 0.5, 0.5, 0.5);
                }

                // --- Frostbite ---
                int frostLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FROSTBITE.get(), weapon);
                if (frostLvl > 0) {
                    int duration = 20 + frostLvl * 16;
                    int amp = Math.max(0, (int) (frostLvl * 0.4F));
                    target.addPotionEffect(new EffectInstance(Effects.SLOWNESS, duration, amp));
                    target.addPotionEffect(new EffectInstance(Effects.WEAKNESS, duration, amp));
                    target.addPotionEffect(new EffectInstance(ModEffects.FROSTBITTEN.get(), duration, frostLvl));
                    target.attackEntityFrom(ModDamageSources.FROSTBITE, 1.0F * frostLvl);
                    sw.spawnParticle(ParticleTypes.CLOUD, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 20, 0.5, 1.0, 0.5, 0.05);
                }

                // --- Plague Doctor ---
                int plagueLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.PLAGUE_DOCTOR.get(), weapon);
                if (plagueLvl > 0) {
                    float plagueChance = plagueLvl == 1 ? 0.09F : plagueLvl == 2 ? 0.17F : 0.25F;
                    if (npc.getRNG().nextFloat() < plagueChance && !target.isPotionActive(ModEffects.PLAGUE.get())) {
                        target.getPersistentData().putBoolean("HasPlague", true);
                        target.addPotionEffect(new EffectInstance(ModEffects.PLAGUE.get(), 4800, plagueLvl - 1, false, true, true));
                        sw.spawnParticle(ParticleTypes.SNEEZE, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 30, 0.5, 1.0, 0.5, 0.1);
                    }
                }

                // --- Soul Eater ---
                int soulLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SOUL_EATER.get(), weapon);
                if (soulLvl > 0) {
                    CompoundNBT nbt = weapon.getOrCreateTag();
                    int souls = nbt.getInt("Souls");
                    if (souls > 0) {
                        dmgMult *= (1.0F + 0.2F * souls);
                        nbt.putInt("Souls", souls - 1);
                        sw.spawnParticle(ParticleTypes.SOUL_FIRE_FLAME, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 6, 0.25, 0.35, 0.25, 0.01);
                    }
                }

                // --- Unstable Edge: 30% шанс крита ---
                int ueLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.UNSTABLE_EDGE.get(), weapon);
                if (ueLvl > 0) {
                    boolean crit = npc.getRNG().nextFloat() < 0.30F;
                    dmgMult *= UnstableEdgeHandler.rollMultiplier(ueLvl, crit, npc.getRNG());
                    if (crit) {
                        sw.spawnParticle(ParticleTypes.ENCHANTED_HIT,
                                target.getPosX(), target.getPosY() + target.getHeight() * 0.6D,
                                target.getPosZ(), 18, 0.35D, 0.45D, 0.35D, 0.12D);
                    }
                }

                event.setAmount((baseAmount + bonus) * dmgMult);
            } finally {
                data.remove(GUARD);
            }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-NpcEnchantHandler-onNpcAttack] Error in event", t);
        }
    }

    @SubscribeEvent
    public void onNpcKill(LivingDeathEvent event) {
        try {
            Entity trueSrc = event.getSource().getTrueSource();
            if (!(trueSrc instanceof LivingEntity) || trueSrc instanceof PlayerEntity) return;
            LivingEntity npc = (LivingEntity) trueSrc;
            if (!CustomNpcChaosBridge.isCustomNpc(npc) || npc.world.isRemote) return;

            ItemStack weapon = npc.getHeldItemMainhand();
            int soulLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SOUL_EATER.get(), weapon);
            if (soulLvl > 0) {
                CompoundNBT nbt = weapon.getOrCreateTag();
                long time = npc.world.getGameTime();
                if (time - nbt.getLong("LastSoulTime") > 20) {
                    nbt.putInt("Souls", Math.min(nbt.getInt("Souls") + 1, 5));
                    nbt.putLong("LastSoulTime", time);
                    npc.heal(0.5F * soulLvl);
                    ((ServerWorld) npc.world).spawnParticle(ParticleTypes.SOUL,
                            npc.getPosX(), npc.getPosY() + 1.0, npc.getPosZ(), 15, 0.5, 0.5, 0.5, 0.1);
                }
            }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-NpcEnchantHandler-onNpcKill] Error in event", t);
        }
    }

    private static void spawnLightning(ServerWorld world, Entity target) {
        net.minecraft.entity.effect.LightningBoltEntity bolt =
                net.minecraft.entity.EntityType.LIGHTNING_BOLT.create(world);
        if (bolt != null) {
            bolt.setEffectOnly(true);
            bolt.setPosition(target.getPosX(), target.getPosY(), target.getPosZ());
            world.addEntity(bolt);
        }
    }
}
