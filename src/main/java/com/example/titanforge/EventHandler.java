package com.example.titanforge;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.screen.inventory.CreativeScreen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.attributes.ModifiableAttributeInstance;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.UUID;

public class EventHandler {
    private static final java.util.HashSet<Integer> bloodPactProcessing = new java.util.HashSet<>();

    private static final String SOUL_COPY = "IsSoulCopy";
    private static final String SOUL_COPY_LIFE = "SoulCopyLife";
    private static final String SOUL_OWNER = "SoulOwner";

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof CreativeScreen) {
            event.addWidget(new Button(((CreativeScreen) event.getGui()).width - 100, 10, 80, 20,
                    new StringTextComponent("Enchanter"),
                    button -> {
                        try {
                            NetworkHandler.INSTANCE.sendToServer(new OpenGuiPacket());
                        } catch (Throwable t) {
                            try {
                                net.minecraft.client.Minecraft.getInstance().player.sendChatMessage("/enchanter");
                            } catch (Throwable ignored) {}
                        }
                    }));
        }
    }

    @SubscribeEvent
    public void onAttack(LivingHurtEvent event) {
        try {
        LivingEntity victim = event.getEntityLiving();
        if (victim.getPersistentData().getBoolean("IsSoulCopy")) {
            Entity src = event.getSource().getTrueSource();
            if (src instanceof PlayerEntity) {
                UUID ownerId = victim.getPersistentData().getUniqueId("SoulOwner");
                if (ownerId != null && ((PlayerEntity) src).getUniqueID().equals(ownerId)) {
                    event.setCanceled(true);
                    return;
                }
            }
        }

        if (ChronoAnchorManager.captureDamage(event.getEntityLiving(), event.getAmount())) {
            event.setCanceled(true);
            return;
        }

        if (!(event.getSource().getTrueSource() instanceof PlayerEntity)) return;
        if (event.getSource().isExplosion()) return;
        PlayerEntity player = (PlayerEntity) event.getSource().getTrueSource();
        ItemStack weapon = player.getHeldItemMainhand();
        LivingEntity target = event.getEntityLiving();
        float baseAmount = event.getAmount();
        float dmgMult = 1.0F;

        if (ChaosDevourHandler.isOwnThrall(target, player)) {
            event.setCanceled(true);
            return;
        }
            // Titan's Wrath
            int titansWrathLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.TITANS_WRATH.get(), weapon);
            boolean vanillaCrit = player.fallDistance > 0.0F && !player.isOnGround() && !player.isInWater() && !player.isOnLadder();
            if (titansWrathLvl > 0 && vanillaCrit && !player.world.isRemote) {
                long now = player.world.getGameTime();
                if (now > player.getPersistentData().getLong("TF_TitanWrathCD")) {
                    player.getPersistentData().putLong("TF_TitanWrathCD", now + 220L);
                    float splashDamage = 3.0F + titansWrathLvl * 1.5F;
                    for (LivingEntity e : ((ServerWorld)player.world).getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(target.getPosition()).grow(3.0))) {
                        if (e != player && !ChaosDevourHandler.isOwnThrall(e, player)) {
                            e.hurtResistantTime = 0;
                            e.attackEntityFrom(DamageSource.causePlayerDamage(player), splashDamage);
                        }
                    }
                    event.setAmount(baseAmount + 2.0F * titansWrathLvl);
                    player.world.playSound(null, target.getPosX(), target.getPosY(), target.getPosZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8F, 1.0F);
                    ((ServerWorld)player.world).spawnParticle(ParticleTypes.EXPLOSION, target.getPosX(), target.getPosY() + 0.5, target.getPosZ(), 4, 0.3, 0.3, 0.3, 0.02);
                    ((ServerWorld)player.world).spawnParticle(ParticleTypes.SMOKE, target.getPosX(), target.getPosY() + 0.5, target.getPosZ(), 30, 0.7, 0.5, 0.7, 0.08);
                }
            }

            // Vampirism
            int vampLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.VAMPIRISM.get(), weapon);
            if (vampLvl > 0 && !player.world.isRemote) {
                float healAmt = baseAmount * 0.1F * vampLvl;
                if (player.getActivePotionEffect(ModEffects.BLOOD_FRENZY.get()) != null) {
                    healAmt = Math.max(healAmt, baseAmount * 0.25F);
                }
                player.heal(healAmt);
                ServerWorld sw = (ServerWorld) player.world;
                sw.spawnParticle(ParticleTypes.HEART, player.getPosX(), player.getPosY() + 1.0, player.getPosZ(), 5, 0.5, 0.5, 0.5, 0.1);
                sw.spawnParticle(ParticleTypes.CRIT, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 10, 0.3, 0.5, 0.3, 0.05);
            }

            // Chain Lightning
            int chainLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CHAIN_LIGHTNING.get(), weapon);
            if (chainLvl > 0 && !player.world.isRemote) {
                ServerWorld sw = (ServerWorld) player.world;
                long now = player.world.getGameTime();
                if (now >= player.getPersistentData().getLong("TF_ChainLightningCD")) {
                    player.getPersistentData().putLong("TF_ChainLightningCD", now + 40L);
                    spawnLightning(sw, target);
                    int jumps = 0;
                    for (LivingEntity e : sw.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(target.getPosition()).grow(5))) {
                        if (e != target && e != player && !(e instanceof PlayerEntity) && !ChaosDevourHandler.isThrall(e)) {
                            spawnLightning(sw, e);
                            e.attackEntityFrom(DamageSource.LIGHTNING_BOLT, baseAmount * 0.5F);
                            sw.spawnParticle(ParticleTypes.END_ROD, e.getPosX(), e.getPosY() + 1.0, e.getPosZ(), 12, 0.4, 0.6, 0.4, 0.05);
                            if (++jumps >= 3) break;
                        }
                    }
                }
            }

            // Void Curse — 20s cooldown, no visual recharge on item
            int curseLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.VOID_CURSE.get(), weapon);
            if (curseLvl > 0 && !player.world.isRemote) {
                long now = player.world.getGameTime();
                if (now >= player.getPersistentData().getLong("TF_VoidCurseCD")) {
                    player.getPersistentData().putLong("TF_VoidCurseCD", now + 400L);
                    target.addPotionEffect(new EffectInstance(Effects.LEVITATION, 60, 1));
                    target.world.playSound(null, target.getPosX(), target.getPosY(), target.getPosZ(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 0.8F, 1.0F);
                    ServerWorld sw = (ServerWorld) player.world;
                    sw.spawnParticle(ParticleTypes.DRAGON_BREATH, target.getPosX(), target.getPosY() + 0.6, target.getPosZ(), 18, 0.35, 0.45, 0.35, 0.02);
                    sw.spawnParticle(ParticleTypes.REVERSE_PORTAL, target.getPosX(), target.getPosY() + 0.8, target.getPosZ(), 24, 0.45, 0.65, 0.45, 0.06);
                    sw.spawnParticle(ParticleTypes.SMOKE, target.getPosX(), target.getPosY() + 0.2, target.getPosZ(), 10, 0.25, 0.2, 0.25, 0.02);
                }
            }

            // Executioner
            int execLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.EXECUTIONER.get(), weapon);
            if (execLvl > 0 && target.getHealth() <= target.getMaxHealth() * 0.25F) {
                if (!(target instanceof net.minecraft.entity.player.PlayerEntity)) {
                    dmgMult *= (2.0F + execLvl);
                    target.world.playSound(null, target.getPosX(), target.getPosY(), target.getPosZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                    if (!target.world.isRemote) {
                        ((ServerWorld)target.world).spawnParticle(ParticleTypes.CRIT, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 30, 0.5, 0.5, 0.5, 0.5);
                    }
                }
            }

            // Frostbite
            int frostLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.FROSTBITE.get(), weapon);
            if (frostLvl > 0) {
                int duration = 20 + frostLvl * 16;
                int amp = Math.max(0, (int)(frostLvl * 0.4F));
                target.addPotionEffect(new EffectInstance(Effects.SLOWNESS, duration, amp));
                target.addPotionEffect(new EffectInstance(Effects.WEAKNESS, duration, amp));
                target.addPotionEffect(new EffectInstance(ModEffects.FROSTBITTEN.get(), duration, frostLvl));
                target.attackEntityFrom(ModDamageSources.FROSTBITE, 1.0F * frostLvl);
                target.world.playSound(null, target.getPosX(), target.getPosY(), target.getPosZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0F, 1.5F);
                if (!player.world.isRemote) {
                    ((ServerWorld)player.world).spawnParticle(ParticleTypes.CLOUD, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 20, 0.5, 1.0, 0.5, 0.05);
                }
            }

            // Blood Pact (toggle mode)
            if (player.getPersistentData().getBoolean("BloodPactActive")) {
                int bloodLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.BLOOD_PACT.get(), weapon);
                if (bloodLvl > 0) {
                    if (bloodPactProcessing.contains(target.getEntityId())) {
                        bloodPactProcessing.remove(target.getEntityId());
                    } else {
                        float drain = bloodLvl == 1 ? 3.0F : bloodLvl == 2 ? 5.0F : 7.0F;
                        float pureDmg = bloodLvl == 1 ? 8.0F : bloodLvl == 2 ? 12.0F : 16.0F;

                        if (player.getHealth() <= 2.0F + drain) {
                            player.getPersistentData().putBoolean("BloodPactActive", false);
                            player.getPersistentData().putLong("BloodPactCooldown", player.world.getGameTime() + 200L);
                            NetworkHandler.INSTANCE.send(
                                net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity) player),
                                new BloodPactSyncPacket(false)
                            );
                            player.sendStatusMessage(new StringTextComponent("\u00A7c\u041A\u0440\u043E\u0432\u0430\u0432\u044B\u0439 \u0414\u043E\u0433\u043E\u0432\u043E\u0440 \u0441\u043E\u0440\u0432\u0430\u043D!"), true);
                        } else {
                            player.setHealth(player.getHealth() - drain);
                            bloodPactProcessing.add(target.getEntityId());
                            target.hurtResistantTime = 0;
                            target.attackEntityFrom(DamageSource.causePlayerDamage(player).setDamageBypassesArmor(), pureDmg);
                            player.world.playSound(null, target.getPosX(), target.getPosY(), target.getPosZ(), SoundEvents.ENTITY_WITHER_HURT, SoundCategory.PLAYERS, 1.0F, 0.5F);
                            if (!player.world.isRemote) {
                                ((ServerWorld)player.world).spawnParticle(ParticleTypes.DAMAGE_INDICATOR, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 15, 0.5, 0.5, 0.5, 0.1);
                            }
                        }
                    }
                }
            }

            // Soul Eater
            int soulLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SOUL_EATER.get(), weapon);
            if (soulLvl > 0) {
                CompoundNBT nbt = weapon.getOrCreateTag();
                int souls = nbt.getInt("Souls");
                if (souls > 0) {
                    dmgMult *= (1.0F + 0.2F * souls);
                    if (!player.world.isRemote) {
                        ((ServerWorld) player.world).spawnParticle(ParticleTypes.SOUL_FIRE_FLAME,
                            target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 6, 0.25, 0.35, 0.25, 0.01);
                    }
                    if (!player.world.isRemote) nbt.putInt("Souls", souls - 1);
                }
            }

            // Quantum Entanglement
            int quantumLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.QUANTUM_ENTANGLEMENT.get(), weapon);
            if (quantumLvl > 0 && !player.world.isRemote) {
                ServerWorld sw = (ServerWorld) player.world;
                for (LivingEntity e : target.world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(target.getPosition()).grow(5))) {
                    if (e != player && e != target && !(e instanceof PlayerEntity) && !ChaosDevourHandler.isThrall(e)) {
                        e.attackEntityFrom(DamageSource.causePlayerDamage(player), baseAmount * 0.5F * quantumLvl);
                        target.addPotionEffect(new EffectInstance(Effects.GLOWING, 40, 0, false, false));
                        e.addPotionEffect(new EffectInstance(Effects.GLOWING, 40, 0, false, false));
                        sw.spawnParticle(ParticleTypes.ENCHANT, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 8, 0.3, 0.4, 0.3, 0.03);
                        sw.spawnParticle(ParticleTypes.ENCHANT, e.getPosX(), e.getPosY() + 1.0, e.getPosZ(), 8, 0.3, 0.4, 0.3, 0.03);
                        break;
                    }
                }
            }

            // Plague Doctor
            int plagueLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.PLAGUE_DOCTOR.get(), weapon);
            if (plagueLvl > 0 && !player.world.isRemote) {
                boolean isCrit = !player.isOnGround() && player.fallDistance > 0.0F;
                float plagueChance = plagueLvl == 1 ? 0.18F : plagueLvl == 2 ? 0.34F : 0.5F;
                if (isCrit && player.world.rand.nextFloat() < plagueChance && !target.isPotionActive(ModEffects.PLAGUE.get())) {
                    target.getPersistentData().putBoolean("HasPlague", true);
                    target.addPotionEffect(new EffectInstance(ModEffects.PLAGUE.get(), 4800, plagueLvl - 1, false, true, true));
                    ServerWorld sw = (ServerWorld) player.world;
                    sw.spawnParticle(ParticleTypes.SNEEZE, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 30, 0.5, 1.0, 0.5, 0.1);
                    sw.spawnParticle(ParticleTypes.ASH, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 20, 0.5, 1.0, 0.5, 0.05);
                    player.world.playSound(null, target.getPosX(), target.getPosY(), target.getPosZ(), SoundEvents.ENTITY_WITHER_DEATH, SoundCategory.PLAYERS, 0.5F, 0.5F);
                }
            }

        // Shadow Step crit flag applies regardless of cooldown
        CompoundNBT playerData = player.getPersistentData();
        if (playerData.getBoolean("ShadowStepCrit")) {
            dmgMult *= 2.0F;
            playerData.remove("ShadowStepCrit");
        }

        if (dmgMult != 1.0F) event.setAmount(event.getAmount() * dmgMult);

        // Store final damage on target for Blood Frenzy kill healing
        if (!player.world.isRemote) {
            CompoundNBT tData = target.getPersistentData();
            tData.putFloat("TF_LastHitDamage", event.getAmount());
            tData.putLong("TF_LastHitTick", player.world.getGameTime());
            tData.putUniqueId("TF_LastHitBy", player.getUniqueID());
        }
    } catch (Throwable t) {
        TitanForge.LOGGER.error("[TitanForge-EventHandler-onAttack] Error in event", t);
    }}

    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        try {
        DamageSource source = event.getSource();
        if (!(source.getTrueSource() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) source.getTrueSource();
        ItemStack weapon = player.getHeldItemMainhand();

        // Soul Eater
        int soulLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SOUL_EATER.get(), weapon);
        if (soulLvl > 0 && !player.world.isRemote) {
            CompoundNBT nbt = weapon.getOrCreateTag();
            int souls = nbt.getInt("Souls");
            long time = player.world.getGameTime();
            if (time - nbt.getLong("LastSoulTime") > 20) {
                nbt.putInt("Souls", Math.min(souls + 1, 5));
                nbt.putLong("LastSoulTime", time);
                player.heal(0.5F * soulLvl);
                player.addPotionEffect(new EffectInstance(Effects.ABSORPTION, 80, 0, false, true));
                ServerWorld sw = (ServerWorld) player.world;
                sw.spawnParticle(ParticleTypes.SOUL, event.getEntityLiving().getPosX(), event.getEntityLiving().getPosY() + 1.0, event.getEntityLiving().getPosZ(), 15, 0.5, 0.5, 0.5, 0.1);
                sw.spawnParticle(ParticleTypes.SOUL_FIRE_FLAME, player.getPosX(), player.getPosY() + 1.0, player.getPosZ(), 8, 0.4, 0.5, 0.4, 0.03);
                player.world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.PARTICLE_SOUL_ESCAPE, SoundCategory.PLAYERS, 0.8F, 1.1F);
            }
        }

        // Blood Rage (тільки з Chaos Devour)
        if (!player.world.isRemote) {
            int chaosLevel = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CHAOS_DEVOUR.get(), weapon);
            if (chaosLevel <= 0) return;
            CompoundNBT pData = player.getPersistentData();
            int charge = pData.getInt("BloodRageCharge");
            if (charge < 200) {
                LivingEntity dead = event.getEntityLiving();
                int gain = dead instanceof MonsterEntity ? 25 : 10;

                // Multiplier when fighting multiple enemies
                int nearbyHostile = player.world.getEntitiesWithinAABB(MonsterEntity.class, player.getBoundingBox().grow(10.0)).size();
                if (nearbyHostile >= 2) gain = (int)(gain * 1.5);

                charge = Math.min(200, charge + gain);
                pData.putInt("BloodRageCharge", charge);

                // Sync to client
                if (player instanceof net.minecraft.entity.player.ServerPlayerEntity) {
                    NetworkHandler.INSTANCE.send(
                        net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity) player),
                        new BloodRageSyncPacket(charge)
                    );
                }

                // Blood Frenzy at 200
                if (charge >= 200) {
                    pData.putInt("BloodRageCharge", 0);
                    player.addPotionEffect(new EffectInstance(ModEffects.BLOOD_FRENZY.get(), 400, chaosLevel - 1, false, true));
                    player.world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(),
                        SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.0F, 0.8F);
                    ((ServerWorld)player.world).spawnParticle(ParticleTypes.SOUL_FIRE_FLAME,
                        player.getPosX(), player.getPosY() + 1.0, player.getPosZ(), 30, 0.5, 0.8, 0.5, 0.05);
                }
            }
        }

        // Blood Frenzy kill healing (heal from damage dealt to THIS target)
        EffectInstance frenzy = player.getActivePotionEffect(ModEffects.BLOOD_FRENZY.get());
        if (frenzy != null && !player.world.isRemote) {
            LivingEntity dead = event.getEntityLiving();
            CompoundNBT tData = dead.getPersistentData();
            boolean freshHit = tData.getLong("TF_LastHitTick") == player.world.getGameTime()
                    && player.getUniqueID().equals(tData.getUniqueId("TF_LastHitBy"));
            if (freshHit) {
                float dmg = tData.getFloat("TF_LastHitDamage");
                int amp = frenzy.getAmplifier();
                float heal = amp == 0 ? dmg * 0.1F : amp == 1 ? dmg * 0.25F : dmg * 0.5F;
                player.heal(heal);
                ((ServerWorld) player.world).spawnParticle(ParticleTypes.HEART,
                        player.getPosX(), player.getPosY() + 1.0, player.getPosZ(), 10, 0.5, 0.5, 0.5, 0.1);
            }
        }
    } catch (Throwable t) {
        TitanForge.LOGGER.error("[TitanForge-EventHandler-onKill] Error in event", t);
    }}

    @SubscribeEvent
    public void onPotionAdded(PotionEvent.PotionAddedEvent event) {
        try {
            EffectInstance newEffect = event.getPotionEffect();
            if (newEffect.getPotion() == ModEffects.PLAGUE.get() && newEffect.getDuration() < 4800) {
                event.getEntityLiving().removePotionEffect(ModEffects.PLAGUE.get());
                event.getEntityLiving().addPotionEffect(new EffectInstance(ModEffects.PLAGUE.get(), 4800, newEffect.getAmplifier(), false, true, true));
                if (!event.getEntityLiving().world.isRemote) {
                    event.getEntityLiving().getPersistentData().putBoolean("HasPlague", true);
                }
            }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-EventHandler-onPotionAdded] Error in event", t);
        }
    }

    @SubscribeEvent
    public void onArrowImpact(ProjectileImpactEvent event) {
        // Chrono Anchor
        if (event.getEntity() instanceof AbstractArrowEntity) {
            AbstractArrowEntity proj = (AbstractArrowEntity) event.getEntity();
            if (proj.getShooter() instanceof PlayerEntity) {
                PlayerEntity shooter = (PlayerEntity) proj.getShooter();
                ItemStack bow = shooter.getHeldItemMainhand();
                if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CHRONO_ANCHOR.get(), bow) <= 0) {
                    bow = shooter.getHeldItemOffhand();
                }
                int chronoLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CHRONO_ANCHOR.get(), bow);
                if (chronoLvl > 0 && event.getRayTraceResult() instanceof EntityRayTraceResult
                        && ((EntityRayTraceResult) event.getRayTraceResult()).getEntity() instanceof LivingEntity) {
                    LivingEntity target = (LivingEntity) ((EntityRayTraceResult) event.getRayTraceResult()).getEntity();
                    ChronoAnchorManager.freeze(target, 80);
                }
            }
        }

        if (!(event.getEntity() instanceof ArrowEntity)) return;
        ArrowEntity arrow = (ArrowEntity) event.getEntity();
        if (!(arrow.getShooter() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) arrow.getShooter();
        ItemStack bow = player.getHeldItemMainhand();

        // Solar Flare
        int solarLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SOLAR_FLARE.get(), bow);
        if (solarLvl > 0 && event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY
                && ((EntityRayTraceResult) event.getRayTraceResult()).getEntity() instanceof LivingEntity) {
            LivingEntity hit = (LivingEntity) ((EntityRayTraceResult) event.getRayTraceResult()).getEntity();
            hit.getPersistentData().putInt("SolarFireTimer", 4 * solarLvl * 20);
            if (!hit.world.isRemote) {
                ServerWorld sw = (ServerWorld) hit.world;
                sw.spawnParticle(ParticleTypes.SOUL_FIRE_FLAME, hit.getPosX(), hit.getPosY() + 1.0, hit.getPosZ(), 15, 0.3, 0.5, 0.3, 0.1);
                sw.spawnParticle(ParticleTypes.LARGE_SMOKE, hit.getPosX(), hit.getPosY() + 1.0, hit.getPosZ(), 8, 0.3, 0.5, 0.3, 0.03);
                sw.spawnParticle(ParticleTypes.LAVA, hit.getPosX(), hit.getPosY() + 1.0, hit.getPosZ(), 5, 0.3, 0.5, 0.3, 0.1);
            }
            if (hit.isEntityUndead()) {
                hit.attackEntityFrom(DamageSource.causePlayerDamage(player), 3.0F * solarLvl);
            }
        }

        // Solar Flare daytime damage boost
        if (arrow.world.isDaytime()) {
            int solarDayLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SOLAR_FLARE.get(), bow);
            if (solarDayLvl > 0) {
                arrow.setDamage(arrow.getDamage() + 1.5 * solarDayLvl);
                if (!arrow.world.isRemote) {
                    ((ServerWorld)arrow.world).spawnParticle(ParticleTypes.SOUL_FIRE_FLAME, arrow.getPosX(), arrow.getPosY(), arrow.getPosZ(), 10, 0.2, 0.2, 0.2, 0.05);
                }
            }
        }

        // Void Vortex
        int vortexLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.VOID_VORTEX.get(), bow);
        if (vortexLvl > 0 && !arrow.world.isRemote && !player.getCooldownTracker().hasCooldown(bow.getItem())) {
            player.getCooldownTracker().setCooldown(bow.getItem(), 100);
            ServerWorld sw = (ServerWorld) arrow.world;
            Vector3d hitVec = event.getRayTraceResult().getHitVec();
            for (int p = 0; p < 24; p++) {
                double angle = p * (Math.PI * 2 / 15);
                double x = hitVec.x + Math.cos(angle) * 2.0;
                double z = hitVec.z + Math.sin(angle) * 2.0;
                sw.spawnParticle(ParticleTypes.CLOUD, x, hitVec.y + 1.0, z, 1, 0, 0.2, 0, 0.03);
                sw.spawnParticle(ParticleTypes.WHITE_ASH, x, hitVec.y + 1.2, z, 1, 0, 0.25, 0, 0.02);
            }
            VoidVortexManager.spawn(sw, hitVec, player);
        }

        // Ricochet
        int ricochetLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.RICOCHET.get(), bow);
        if (ricochetLvl > 0 && !arrow.world.isRemote && event.getRayTraceResult().getType() == RayTraceResult.Type.ENTITY
                && ((EntityRayTraceResult) event.getRayTraceResult()).getEntity() instanceof LivingEntity) {
            LivingEntity hit = (LivingEntity) ((EntityRayTraceResult) event.getRayTraceResult()).getEntity();
            int bouncesLeft = arrow.getPersistentData().getInt("TF_RicochetBounces");
            if (bouncesLeft <= 0) bouncesLeft = ricochetLvl;
            if (bouncesLeft > 0) {
                LivingEntity nearest = null;
                double nearestDist = 10.0;
                for (LivingEntity e : arrow.world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(hit.getPosition()).grow(10))) {
                    if (e != hit && e != player && !ChaosDevourHandler.isThrall(e)) {
                        double dist = hit.getDistanceSq(e);
                        if (dist < nearestDist * nearestDist) {
                            nearestDist = Math.sqrt(dist);
                            nearest = e;
                        }
                    }
                }
                if (nearest != null) {
                    arrow.remove();
                    ArrowEntity ricochet = new ArrowEntity(arrow.world, player);
                    ricochet.setPosition(hit.getPosX(), hit.getPosYEye() - 0.1, hit.getPosZ());
                    Vector3d dir = nearest.getPositionVec().add(0.0D, nearest.getHeight() * 0.4D, 0.0D)
                            .subtract(hit.getPositionVec().add(0.0D, hit.getHeight() * 0.4D, 0.0D)).normalize();
                    ricochet.setMotion(dir.scale(2.2D));
                    ricochet.setDamage(arrow.getDamage() * (0.6F + ricochetLvl * 0.2F));
                    ricochet.pickupStatus = AbstractArrowEntity.PickupStatus.CREATIVE_ONLY;
                    ricochet.getPersistentData().putInt("TF_RicochetBounces", bouncesLeft - 1);
                    arrow.world.addEntity(ricochet);
                    ((ServerWorld) arrow.world).spawnParticle(ParticleTypes.CRIT, nearest.getPosX(), nearest.getPosY() + 1.0, nearest.getPosZ(), 8, 0.2, 0.4, 0.2, 0.02);
                    ((ServerWorld) arrow.world).spawnParticle(ParticleTypes.SWEEP_ATTACK, hit.getPosX(), hit.getPosY() + 1.0, hit.getPosZ(), 1, 0.0, 0.0, 0.0, 0.0);
                    arrow.world.playSound(null, nearest.getPosX(), nearest.getPosY(), nearest.getPosZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 0.7F, 1.3F);
                }
            }
        }
    }

    @SubscribeEvent
    public void onArrowLoose(ArrowLooseEvent event) {
        PlayerEntity player = event.getPlayer();
        ItemStack bow = event.getBow();
        World world = player.world;

        int clusterLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CLUSTER_SHOT.get(), bow);
        boolean flaming = EnchantmentHelper.getEnchantmentLevel(
                net.minecraft.enchantment.Enchantments.FLAME, bow) > 0;
        if (clusterLvl > 0 && !world.isRemote) {
            for (int i = 0; i < 4; i++) {
                ArrowEntity cluster = new ArrowEntity(world, player);
                cluster.setPosition(player.getPosX(), player.getPosYEye(), player.getPosZ());
                Vector3d spread = new Vector3d(
                    player.getLookVec().x + (world.rand.nextFloat() - 0.5) * 0.5,
                    player.getLookVec().y + (world.rand.nextFloat() - 0.5) * 0.5,
                    player.getLookVec().z + (world.rand.nextFloat() - 0.5) * 0.5
                ).normalize().scale(2.0);
                cluster.setMotion(spread);
                cluster.setDamage(2.0);
                if (flaming) cluster.setFire(100);
                world.addEntity(cluster);
            }
            player.getCooldownTracker().setCooldown(bow.getItem(), 40);
        }
    }

    @SubscribeEvent
    public void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        PlayerEntity player = event.getPlayer();
        if (!player.isSneaking()) return;
        ItemStack weapon = player.getHeldItemMainhand();

        // Blood Pact activation
        int bloodLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.BLOOD_PACT.get(), weapon);
        if (bloodLvl > 0 && !player.world.isRemote) {
            CompoundNBT data = player.getPersistentData();
            boolean isActive = data.getBoolean("BloodPactActive");
            if (!isActive) {
                if (player.world.getGameTime() > data.getLong("BloodPactCooldown")) {
                    data.putBoolean("BloodPactActive", true);
                    NetworkHandler.INSTANCE.send(
                        net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity) player),
                        new BloodPactSyncPacket(true)
                    );
                    player.sendStatusMessage(new StringTextComponent("\u00A74\u041A\u0440\u043E\u0432\u0430\u0432\u044B\u0439 \u0414\u043E\u0433\u043E\u0432\u043E\u0440 \u0430\u043A\u0442\u0438\u0432\u0438\u0440\u043E\u0432\u0430\u043D!"), true);
                    event.setCanceled(true);
                    return;
                } else {
                    player.sendStatusMessage(new StringTextComponent("\u00A7c\u0414\u043E\u0433\u043E\u0432\u043E\u0440 \u043D\u0430 \u043F\u0435\u0440\u0435\u0437\u0430\u0440\u044F\u0434\u043A\u0435!"), true);
                    event.setCanceled(true);
                    return;
                }
            } else {
                data.putBoolean("BloodPactActive", false);
                NetworkHandler.INSTANCE.send(
                    net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity) player),
                    new BloodPactSyncPacket(false)
                );
                player.sendStatusMessage(new StringTextComponent("\u00A77\u041A\u0440\u043E\u0432\u0430\u0432\u044B\u0439 \u0414\u043E\u0433\u043E\u0432\u043E\u0440 \u0434\u0435\u0430\u043A\u0442\u0438\u0432\u0438\u0440\u043E\u0432\u0430\u043D."), true);
                event.setCanceled(true);
                return;
            }
        }

        // Shadow Step
        int shadowLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SHADOW_STEP.get(), weapon);
        if (shadowLvl <= 0) return;

        if (player.getCooldownTracker().hasCooldown(weapon.getItem())) return;

        LivingEntity chosen = null;
        double bestDot = 0.92D;
        Vector3d eye = player.getPositionVec().add(0.0D, player.getEyeHeight(), 0.0D);
        Vector3d look = player.getLookVec().normalize();
        for (LivingEntity e : player.world.getEntitiesWithinAABB(LivingEntity.class, player.getBoundingBox().grow(6.0D + shadowLvl * 2.0D))) {
            if (e == player || !e.isAlive()) continue;
            Vector3d to = e.getPositionVec().add(0.0D, e.getEyeHeight(), 0.0D).subtract(eye);
            double dist = to.length();
            if (dist > 8.0D + shadowLvl * 2.0D) continue;
            double dot = look.dotProduct(to.normalize());
            if (dot > bestDot) {
                bestDot = dot;
                chosen = e;
            }
        }
        if (chosen != null && !player.world.isRemote) {
            Vector3d targetLook = chosen.getLookVec().normalize();
            Vector3d behindVec = chosen.getPositionVec().subtract(targetLook.scale(1.5D));
            BlockPos behind = new BlockPos(behindVec.x, chosen.getPosY(), behindVec.z);
            if (player.world.isAirBlock(behind) || player.world.getBlockState(behind).getCollisionShape(player.world, behind).isEmpty()) {
                player.setPositionAndUpdate(behind.getX() + 0.5, chosen.getPosY(), behind.getZ() + 0.5);
                player.getPersistentData().putBoolean("ShadowStepCrit", true);
                player.world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.0F);
                ((ServerWorld)player.world).spawnParticle(ParticleTypes.SMOKE, player.getPosX(), player.getPosY() + 1.0, player.getPosZ(), 20, 0.3, 0.5, 0.3, 0.05);
                player.getCooldownTracker().setCooldown(weapon.getItem(), 60);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        try {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (!(event.player instanceof PlayerEntity)) return;
        PlayerEntity player = event.player;
        CompoundNBT data = player.getPersistentData();

        if (data.getBoolean("BloodPactActive")) {
            ItemStack weapon = player.getHeldItemMainhand();
            int bloodLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.BLOOD_PACT.get(), weapon);
            if (bloodLvl > 0) {
                // Particles
                if (!player.world.isRemote && player.ticksExisted % 5 == 0) {
                    ((ServerWorld)player.world).spawnParticle(ParticleTypes.DAMAGE_INDICATOR,
                        player.getPosX() + (player.world.rand.nextDouble() - 0.5) * 0.5,
                        player.getPosY() + 1.0,
                        player.getPosZ() + (player.world.rand.nextDouble() - 0.5) * 0.5,
                        2, 0.1, 0.5, 0.1, 0);
                }

                // Attack speed bonus
                if (!player.world.isRemote) {
                    ModifiableAttributeInstance attr = (ModifiableAttributeInstance) player.getAttribute(Attributes.ATTACK_SPEED);
                    if (attr != null) {
                        UUID speedUUID = UUID.nameUUIDFromBytes("BloodPactSpeed".getBytes());
                        attr.removeModifier(speedUUID);
                        double speedBonus = bloodLvl * 0.05;
                        attr.applyNonPersistentModifier(new AttributeModifier(speedUUID, "BloodPactSpeed", speedBonus, AttributeModifier.Operation.MULTIPLY_TOTAL));
                    }
                }
            } else {
                data.putBoolean("BloodPactActive", false);
            }
        } else {
            if (!player.world.isRemote) {
                ModifiableAttributeInstance attr = (ModifiableAttributeInstance) player.getAttribute(Attributes.ATTACK_SPEED);
                if (attr != null) {
                    UUID speedUUID = UUID.nameUUIDFromBytes("BloodPactSpeed".getBytes());
                    attr.removeModifier(speedUUID);
                }
            }
        }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-EventHandler-onPlayerTick] Error in event", t);
        }
    }

    @SubscribeEvent
    public void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        try {
        PlayerEntity player = event.getPlayer();
        if (player.getPersistentData().getBoolean("BloodPactActive")) {
            if (player.world.isRemote) {
                NetworkHandler.INSTANCE.sendToServer(new SwingPacket());
            }
        }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-EventHandler-onLeftClickEmpty] Error in event", t);
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        PlayerEntity player = event.getPlayer();
        World world = player.world;
        BlockPos pos = event.getPos();
        BlockState state = event.getState();
        ItemStack tool = player.getHeldItemMainhand();

        // Telekinesis
        int teleLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.TELEKINESIS.get(), tool);
        if (teleLvl > 0 && !world.isRemote) {
            event.setCanceled(true);
            ServerWorld sw = (ServerWorld) world;
            sw.spawnParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 5, 0.2, 0.2, 0.2, 0.05);

            List<ItemStack> drops = Block.getDrops(state, sw, pos, world.getTileEntity(pos), player, tool);
            for (ItemStack drop : drops) {
                if (!player.inventory.addItemStackToInventory(drop)) {
                    Block.spawnAsEntity(world, pos, drop);
                }
            }
            world.destroyBlock(pos, false, player);
            if (tool.isDamageable()) tool.damageItem(1, player, (p) -> p.sendBreakAnimation(EquipmentSlotType.MAINHAND));
            return;
        }

        // Abyssal Yield
        int abyssLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.ABYSSAL_YIELD.get(), tool);
        if (abyssLvl > 0 && (state.getBlock() == Blocks.STONE || state.getBlock() == Blocks.NETHERRACK)) {
            if (world.rand.nextFloat() < 0.01F * abyssLvl) {
                ItemStack rare = world.rand.nextBoolean() ? new ItemStack(Items.DIAMOND) : new ItemStack(Items.EMERALD);
                Block.spawnAsEntity(world, pos, rare);
                world.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        }

        // Sonic Haste
        int sonicLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.SONIC_HASTE.get(), tool);
        if (sonicLvl > 0 && !world.isRemote) {
            player.addPotionEffect(new EffectInstance(Effects.SPEED, 60, sonicLvl));
            player.addPotionEffect(new EffectInstance(Effects.JUMP_BOOST, 60, sonicLvl));
            ((ServerWorld)world).spawnParticle(ParticleTypes.SNEEZE, player.getPosX(), player.getPosY() + 0.1, player.getPosZ(), 10, 0.3, 0.1, 0.3, 0.05);
        }
    }

    @SubscribeEvent
    public void onEntityUpdate(LivingEvent.LivingUpdateEvent event) {
        try {
            LivingEntity entity = event.getEntityLiving();
            if (entity.world.isRemote) return;

            ChronoAnchorManager.tick(entity);

            int timer = entity.getPersistentData().getInt("SolarFireTimer");
            if (timer > 0) {
                entity.getPersistentData().putInt("SolarFireTimer", timer - 1);
                if (timer % 20 == 0) {
                    entity.attackEntityFrom(ModDamageSources.SOLAR_FIRE, 1.5F);
                    ((ServerWorld)entity.world).spawnParticle(ParticleTypes.SOUL_FIRE_FLAME, entity.getPosX(), entity.getPosY() + 1.0, entity.getPosZ(), 5, 0.3, 0.3, 0.3, 0.05);
                    ((ServerWorld)entity.world).spawnParticle(ParticleTypes.LARGE_SMOKE, entity.getPosX(), entity.getPosY() + 1.0, entity.getPosZ(), 3, 0.2, 0.3, 0.2, 0.03);
                }
            }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-EventHandler-onEntityUpdate] Error in event", t);
        }
    }

    @SubscribeEvent
    public void onLogout(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {
        bloodPactProcessing.remove(event.getPlayer().getEntityId());
        ZeusStormManager.clear(event.getPlayer().getUniqueID());
    }

    private void spawnLightning(ServerWorld world, Entity target) {
        LightningBoltEntity bolt = EntityType.LIGHTNING_BOLT.create(world);
        if (bolt != null) {
            bolt.setEffectOnly(true);
            bolt.setPosition(target.getPosX(), target.getPosY(), target.getPosZ());
            world.addEntity(bolt);
        }
    }
}
