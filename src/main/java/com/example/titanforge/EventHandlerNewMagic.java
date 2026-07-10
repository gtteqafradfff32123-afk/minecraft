package com.example.titanforge;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.block.Blocks;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class EventHandlerNewMagic {

    @SubscribeEvent
    public void onAttack(LivingHurtEvent event) {
        try {
        if (event.getSource().getTrueSource() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getSource().getTrueSource();
            ItemStack weapon = player.getHeldItemMainhand();
            LivingEntity target = event.getEntityLiving();
            World world = player.world;
            CompoundNBT pData = player.getPersistentData();

            // 1. Absolute Blood
            int bloodLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.ABSOLUTE_BLOOD.get(), weapon);
            if (bloodLvl > 0) {
                if (world.getGameTime() >= pData.getLong("BloodCooldown")) {
                int charge = pData.getInt("BloodCharge");
                charge++;
                pData.putInt("BloodCharge", charge);
                pData.putLong("BloodLastHit", world.getGameTime());

                int needCharge = 12 - bloodLvl * 2;
                if (charge >= needCharge) {
                    pData.putInt("BloodCharge", 0);
                    float radius = 2.0F + bloodLvl;
                    world.createExplosion(player, target.getPosX(), target.getPosY(), target.getPosZ(), radius, false, Explosion.Mode.NONE);
                    List<LivingEntity> enemies = world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(target.getPosition()).grow(radius + 2));
                    float totalHeal = 0F, healCap = player.getMaxHealth();
                    for (LivingEntity e : enemies) {
                        if (e != player && !(e instanceof PlayerEntity)) {
                            e.hurtResistantTime = 0;
                            e.attackEntityFrom(DamageSource.causePlayerDamage(player).setDamageBypassesArmor(), 5.0F + bloodLvl * 5.0F);
                            e.addPotionEffect(new EffectInstance(Effects.WITHER, 60 + bloodLvl * 40, bloodLvl));
                            totalHeal += e.getMaxHealth() * (0.05F + bloodLvl * 0.05F);
                        }
                    }
                    if (!world.isRemote) {
                        for (int i = 0; i < 48; i++) {
                            double a = i * (Math.PI * 2 / 48.0D);
                            double y = player.getPosY() + 0.6D + (i % 8) * 0.18D;
                            double x = player.getPosX() + Math.cos(a) * radius;
                            double z = player.getPosZ() + Math.sin(a) * radius;
                            ((ServerWorld) world).spawnParticle(ParticleTypes.DAMAGE_INDICATOR, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
                        }
                    }
                    player.heal(Math.min(totalHeal, healCap));
                    pData.putLong("BloodCooldown", world.getGameTime() + (700L - bloodLvl * 100L));

                    if (!world.isRemote) {
                        for (LivingEntity e : enemies) {
                            if (e instanceof PlayerEntity && e != player) {
                                NetworkHandler.INSTANCE.send(
                                    net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity) e),
                                    new ScreenEffectPacket(1, 20));
                            }
                        }
                    }
                }
            }}

            // 2. Harvest of Agony
            int agonyLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.HARVEST_OF_AGONY.get(), weapon);
            if (agonyLvl > 0) {
                float threshold = 0.05F + agonyLvl * 0.1F;
                if (player.getHealth() <= player.getMaxHealth() * threshold) {
                    if (world.getGameTime() > pData.getLong("AgonyCooldown") && !pData.getBoolean("AgonyMode")) {
                        int duration = 100 + agonyLvl * 40;
                        pData.putBoolean("AgonyMode", true);
                        pData.putInt("AgonyLevel", agonyLvl);
                        pData.putLong("AgonyModeEnd", world.getGameTime() + duration);
                        player.addPotionEffect(new EffectInstance(Effects.STRENGTH, duration, agonyLvl + 1));
                        player.addPotionEffect(new EffectInstance(Effects.HUNGER, duration, 0));
                        if (!world.isRemote) {
                            NetworkHandler.INSTANCE.send(
                                net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity) player),
                                new ScreenEffectPacket(4, 140));
                            ((ServerWorld) world).spawnParticle(ParticleTypes.CRIT, player.getPosX(), player.getPosY() + 1.0, player.getPosZ(), 24, 0.45, 0.65, 0.45, 0.03);
                        }
                    }
                }
            }

            // 3. Black Singularity
            int singLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.BLACK_SINGULARITY.get(), weapon);
            if (singLvl > 0) {
                boolean isCrit = player.fallDistance > 0.0F && !player.isOnGround();
                if (isCrit && world.getGameTime() > pData.getLong("SingularityCooldown")) {
                    pData.putLong("SingularityCooldown", world.getGameTime() + (500L - singLvl * 100L));
                    int ticks = 40 + singLvl * 20;
                    double radius = 4.0 + singLvl * 2.0;
                    spawnBlackHole(world, target.getPositionVec(), ticks, radius, singLvl);
                }
            }

            // 5. Hierarchy of Ash
            int ashLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.HIERARCHY_OF_ASH.get(), weapon);
            if (ashLvl > 0) {
                boolean isCrit = player.fallDistance > 0.0F && !player.isOnGround();
                float chance = 0.15F + ashLvl * 0.1F;
                if (isCrit && world.rand.nextFloat() < chance && world.getGameTime() > pData.getLong("AshCooldown")) {
                    pData.putLong("AshCooldown", world.getGameTime() + (300L - ashLvl * 60L));
                    target.getPersistentData().putInt("BlackFireTimer", 60 + ashLvl * 60);
                }
            }

            // 6. Leviathan Wrath
            int leviLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.LEVIATHAN_WRATH.get(), weapon);
            if (leviLvl > 0) {
                boolean isJump = player.fallDistance > 0.0F && !player.isOnGround();
                if (isJump && world.getGameTime() > pData.getLong("LeviathanCooldown")) {
                    pData.putLong("LeviathanCooldown", world.getGameTime() + (1100L - leviLvl * 200L));
                    double radius = 3.0 + leviLvl * 2.0;
                    List<LivingEntity> enemies = world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(player.getPosition()).grow(radius));
                    if (!world.isRemote) {
                        for (int i = 0; i < 20; i++) {
                            double a = i * (Math.PI * 2 / 20.0D);
                            ((ServerWorld) world).spawnParticle(ParticleTypes.CLOUD,
                                player.getPosX() + Math.cos(a) * radius, player.getPosY() + 0.1D, player.getPosZ() + Math.sin(a) * radius,
                                1, 0.0, 0.02, 0.0, 0.01);
                        }
                    }
                    for (LivingEntity e : enemies) {
                        if (e != player) {
                            e.setMotion(e.getMotion().add(0, 0.6 + leviLvl * 0.2, 0));
                            e.attackEntityFrom(DamageSource.causePlayerDamage(player).setDamageBypassesArmor(), 4.0F + leviLvl * 4.0F);
                        }
                    }
                }
            }

            // 7. Chaos Puppet
            int puppetLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CHAOS_PUPPET.get(), weapon);
            if (puppetLvl > 0 && target.isAlive()) {
                if (world.getGameTime() > pData.getLong("PuppetCooldown")) {
                    int duration = 100 + puppetLvl * 100;
                    int cd = 1200 - puppetLvl * 200;
                    target.getPersistentData().putInt("IsPuppet", duration);
                    target.addPotionEffect(new EffectInstance(Effects.SPEED, duration, puppetLvl));
                    target.addPotionEffect(new EffectInstance(Effects.STRENGTH, duration, puppetLvl));
                    target.addPotionEffect(new EffectInstance(Effects.GLOWING, duration, 0));
                    if (target instanceof MobEntity) {
                        MobEntity pm = (MobEntity) target;
                        pm.setAttackTarget(null);
                        pm.setRevengeTarget(null);
                        pm.getPersistentData().putUniqueId("PuppetOwner", player.getUniqueID());
                    }
                    pData.putLong("PuppetCooldown", world.getGameTime() + cd);
                }
            }

            if (pData.getBoolean("PhaseRuptureMode")) {
                event.setCanceled(true);
            }
        }
    } catch (Throwable t) {
        System.out.println("[TitanForge-NewMagic-onAttack] " + t.getClass().getName() + ": " + t.getMessage());
        t.printStackTrace();
    }}

    @SubscribeEvent
    public void onLivingHurt(LivingHurtEvent event) {
        try {
        if (event.getEntityLiving() instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) event.getEntityLiving();
            CompoundNBT pData = player.getPersistentData();

            // 4. Phase Rupture
            ItemStack weapon = player.getHeldItemMainhand();
            int phaseLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.PHASE_RUPTURE.get(), weapon);
            if (phaseLvl > 0) {
                if (player.getHealth() - event.getAmount() <= 0 && !pData.getBoolean("PhaseRuptureMode")) {
                    if (player.world.getGameTime() > pData.getLong("PhaseRuptureCooldown")) {
                        event.setCanceled(true);
                        int duration = 60 + phaseLvl * 20;
                        int cd = 4800 - phaseLvl * 600;
                        pData.putBoolean("PhaseRuptureMode", true);
                        pData.putInt("PhaseRuptureLevel", phaseLvl);
                        pData.putLong("PhaseRuptureEnd", player.world.getGameTime() + duration);
                        pData.putLong("PhaseRuptureCooldown", player.world.getGameTime() + cd);
                        player.setHealth(Math.max(1.0F, player.getHealth()));
                        player.addPotionEffect(new EffectInstance(Effects.SPEED, duration, phaseLvl + 2));
                        player.addPotionEffect(new EffectInstance(Effects.INVISIBILITY, duration, 0, false, false));
                        player.setNoGravity(true);
                        player.world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 0.5F);
                        if (!player.world.isRemote) {
                            NetworkHandler.INSTANCE.send(
                                net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> (net.minecraft.entity.player.ServerPlayerEntity) player),
                                new ScreenEffectPacket(3, 80));
                        }
                    }
                }
            }

            // 9. Abyss Aegis
            ItemStack chest = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
            int aegisLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.ABYSS_AEGIS.get(), chest);
            if (aegisLvl > 0) {
                int shields = pData.getInt("AegisShields");
                if (shields > 0) {
                    float reduction = 0.8F - aegisLvl * 0.1F;
                    event.setAmount(event.getAmount() * reduction);
                    pData.putInt("AegisShields", shields - 1);
                    pData.putInt("AegisEnergy", pData.getInt("AegisEnergy") + 10 + aegisLvl * 10);
                    if (shields - 1 == 0) pData.putLong("AegisResetTimer", player.world.getGameTime() + (1400L - aegisLvl * 200L));
                }
            }
        }
    } catch (Throwable t) {
        System.out.println("[TitanForge-NewMagic-onLivingHurt] " + t.getClass().getName() + ": " + t.getMessage());
        t.printStackTrace();
    }}

    @SubscribeEvent
    public void onEntityUpdate(LivingEvent.LivingUpdateEvent event) {
        try {
        LivingEntity entity = event.getEntityLiving();
        World world = entity.world;

        if (entity instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) entity;
            CompoundNBT pData = player.getPersistentData();

            if (pData.getInt("BloodCharge") > 0 && world.getGameTime() - pData.getLong("BloodLastHit") > 300L) {
                pData.putInt("BloodCharge", 0);
                player.getFoodStats().setFoodLevel(Math.max(0, player.getFoodStats().getFoodLevel() - 6));
            }

            if (pData.getBoolean("AgonyMode") && world.getGameTime() > pData.getLong("AgonyModeEnd")) {
                int agonyLvl = pData.getInt("AgonyLevel");
                if (agonyLvl <= 0) agonyLvl = 1;
                pData.putBoolean("AgonyMode", false);
                pData.putLong("AgonyCooldown", world.getGameTime() + (2400L - agonyLvl * 600L));
                player.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 2400 - agonyLvl * 600, 0));
            }

            if (pData.getBoolean("PhaseRuptureMode") && world.getGameTime() > pData.getLong("PhaseRuptureEnd")) {
                pData.putBoolean("PhaseRuptureMode", false);
                player.setNoGravity(false);
                player.removePotionEffect(Effects.INVISIBILITY);
            }

            if (pData.contains("AegisResetTimer") && world.getGameTime() > pData.getLong("AegisResetTimer")) {
                pData.remove("AegisResetTimer");
                pData.putInt("AegisEnergy", 0);
                world.createExplosion(player, player.getPosX(), player.getPosY(), player.getPosZ(), 2.0F, false, Explosion.Mode.NONE);
            }
            ItemStack aegisChest = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
            int aegisLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.ABYSS_AEGIS.get(), aegisChest);
            if (aegisLvl > 0 && !world.isRemote) {
                int shields = pData.getInt("AegisShields");
                for (int i = 0; i < shields; i++) {
                    double angle = world.getGameTime() * 0.12D + i * (Math.PI * 2.0D / Math.max(1, shields));
                    ((ServerWorld) world).spawnParticle(ParticleTypes.DRAGON_BREATH,
                        player.getPosX() + Math.cos(angle) * 1.1D, player.getPosY() + 1.0D, player.getPosZ() + Math.sin(angle) * 1.1D,
                        1, 0.0, 0.0, 0.0, 0.0);
                }
            }
            if (aegisLvl > 0 && pData.getInt("AegisShields") < 2 + aegisLvl && world.getGameTime() % Math.max(20, 120 - aegisLvl * 20) == 0 && pData.getInt("AegisEnergy") == 0) {
                pData.putInt("AegisShields", pData.getInt("AegisShields") + 1);
            }
        }

        if (entity.getPersistentData().contains("IsBlackHole")) {
            if (entity.ticksExisted > 1200 && entity.getPersistentData().contains("IsBlackHole")) { entity.remove(); return; }
            int life = entity.getPersistentData().getInt("IsBlackHole");
            if (life <= 0) {
                entity.remove();
            } else {
                entity.getPersistentData().putInt("IsBlackHole", life - 1);
                if (!world.isRemote) {
                    ServerWorld sw = (ServerWorld) world;
                    sw.spawnParticle(ParticleTypes.DRAGON_BREATH, entity.getPosX(), entity.getPosY()+1, entity.getPosZ(), 10, 0.5, 0.5, 0.5, 0.1);
                    double radius = entity.getPersistentData().getDouble("BlackHoleRadius");
                    if (radius <= 0) radius = 6.0D;
                    List<LivingEntity> mobs = sw.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(entity.getPosition()).grow(radius));
                    for (LivingEntity m : mobs) {
                        if (m instanceof PlayerEntity) continue;
                        Vector3d pull = entity.getPositionVec().subtract(m.getPositionVec()).normalize().scale(0.8);
                        m.setMotion(m.getMotion().add(pull.x, pull.y + 0.1, pull.z));
                        int dmg = entity.getPersistentData().getInt("BlackHoleDmg");
                        if (dmg <= 0) dmg = 1;
                        m.attackEntityFrom(ModDamageSources.BLACK_HOLE, dmg);
                    }
                    for (BlockPos p : BlockPos.getAllInBoxMutable(entity.getPosition().add(-2, -1, -2), entity.getPosition().add(2, 1, 2))) {
                        if (!sw.isAirBlock(p) && sw.rand.nextFloat() < 0.15F) {
                            sw.spawnParticle(ParticleTypes.ASH, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, 4, 0.15, 0.15, 0.15, 0.01);
                            sw.setBlockState(p, Blocks.AIR.getDefaultState(), 18);
                        }
                    }
                }
            }
        }

        if (entity.getPersistentData().getInt("BlackFireTimer") > 0) {
            int timer = entity.getPersistentData().getInt("BlackFireTimer") - 1;
            entity.getPersistentData().putInt("BlackFireTimer", timer);
            entity.setFire(1);
            if (world.isRemote) {
                world.addParticle(ParticleTypes.LARGE_SMOKE, entity.getPosX(), entity.getPosY()+1, entity.getPosZ(), 0, 0.1, 0);
                world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, entity.getPosX(), entity.getPosY()+1, entity.getPosZ(), 0.0, 0.04, 0.0);
            }
            if (timer % 20 == 0 && !world.isRemote) {
                entity.attackEntityFrom(ModDamageSources.BLACK_FIRE, 2.0F);
                List<LivingEntity> nearby = world.getEntitiesWithinAABB(LivingEntity.class, entity.getBoundingBox().grow(1.0));
                for (LivingEntity m : nearby) {
                    if (m != entity && m.getPersistentData().getInt("BlackFireTimer") == 0) {
                        m.getPersistentData().putInt("BlackFireTimer", 60);
                    }
                }
            }
        }

        if (entity.getPersistentData().getInt("IsPuppet") > 0) {
            int time = entity.getPersistentData().getInt("IsPuppet") - 1;
            entity.getPersistentData().putInt("IsPuppet", time);
            if (entity instanceof MonsterEntity && time > 0) {
                MonsterEntity puppet = (MonsterEntity) entity;
                if (puppet.getAttackTarget() instanceof PlayerEntity) puppet.setAttackTarget(null);
                if (puppet.getAttackTarget() == null || !puppet.getAttackTarget().isAlive()) {
                    List<MobEntity> enemies = world.getEntitiesWithinAABB(MobEntity.class, entity.getBoundingBox().grow(15));
                    for (MobEntity m : enemies) {
                        if (m != puppet && m.isAlive() && m.getPersistentData().getInt("IsPuppet") == 0) {
                            puppet.setAttackTarget(m);
                            break;
                        }
                    }
                }
                if (!world.isRemote) {
                    ((ServerWorld) world).spawnParticle(ParticleTypes.WITCH, entity.getPosX(), entity.getPosY()+1, entity.getPosZ(), 3, 0.2, 0.4, 0.2, 0.01);
                }
            }
            if (time == 0) {
                entity.remove();
                world.createExplosion(entity, entity.getPosX(), entity.getPosY(), entity.getPosZ(), 2.0F, false, Explosion.Mode.NONE);
            }
        }

        if (entity instanceof LivingEntity && !entity.world.isRemote) {
            List<PlayerEntity> players = entity.world.getEntitiesWithinAABB(PlayerEntity.class, entity.getBoundingBox().grow(5.0));
            for (PlayerEntity p : players) {
                ItemStack helmet = p.getItemStackFromSlot(EquipmentSlotType.HEAD);
                if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.GAZE_OF_VOID.get(), helmet) > 0) {
                    int gazeLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.GAZE_OF_VOID.get(), helmet);
                    Vector3d look = p.getLookVec().normalize();
                    Vector3d toMob = entity.getPositionVec().subtract(p.getPositionVec()).normalize();
                    if (look.dotProduct(toMob) > 0.95) {
                        if (entity.getPersistentData().getLong("GazeCooldown") < entity.world.getGameTime()) {
                            int cd = 600 - gazeLvl * 100;
                            int duration = 20 + gazeLvl * 20;
                            entity.getPersistentData().putLong("GazeCooldown", entity.world.getGameTime() + cd);
                            entity.addPotionEffect(new EffectInstance(Effects.SLOWNESS, duration, 255));
                            entity.addPotionEffect(new EffectInstance(Effects.MINING_FATIGUE, duration, 255));
                            if (p.getFoodStats().getFoodLevel() > 0) p.getFoodStats().setFoodLevel(p.getFoodStats().getFoodLevel() - gazeLvl);
                            ((ServerWorld)entity.world).spawnParticle(ParticleTypes.ASH, entity.getPosX(), entity.getPosY()+1, entity.getPosZ(), 15, 0.3, 0.5, 0.3, 0.05);
                        }
                    }
                }
            }
        }
    } catch (Throwable t) {
        System.out.println("[TitanForge-NewMagic-onEntityUpdate] " + t.getClass().getName() + ": " + t.getMessage());
        t.printStackTrace();
    }}

    @SubscribeEvent
    public void onKill(LivingDeathEvent event) {
        try {
        LivingEntity dead = event.getEntityLiving();
        World world = dead.world;
        if (!world.isRemote) {
            boolean isChain = dead.getPersistentData().getBoolean("NecropolisEcho");
            int echoLvl = dead.getPersistentData().getInt("NecropolisEchoLevel");
            if (echoLvl <= 0) echoLvl = 1;

            if (!isChain && event.getSource().getTrueSource() instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) event.getSource().getTrueSource();
                ItemStack weapon = player.getHeldItemMainhand();
                echoLvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.NECROPOLIS_ECHO.get(), weapon);
                float chance = -0.05F + echoLvl * 0.15F;
                if (echoLvl > 0 && world.rand.nextFloat() < chance) {
                    isChain = true;
                }
            }

            if (isChain) {
                int generation = dead.getPersistentData().getInt("NecropolisGen");
                if (generation < 3) {
                    world.createExplosion(null, dead.getPosX(), dead.getPosY(), dead.getPosZ(), 2.0F + echoLvl, false, Explosion.Mode.NONE);
                    ((ServerWorld) world).spawnParticle(ParticleTypes.SNEEZE, dead.getPosX(), dead.getPosY() + 1.0, dead.getPosZ(), 60, 1.2, 1.0, 1.2, 0.08);
                    for (LivingEntity e : world.getEntitiesWithinAABB(LivingEntity.class, new AxisAlignedBB(dead.getPosition()).grow(2 + echoLvl))) {
                        if (e instanceof PlayerEntity) continue;
                        e.attackEntityFrom(ModDamageSources.NECROPOLIS_ECHO, 5.0F + echoLvl * 5.0F);
                        e.addPotionEffect(new EffectInstance(ModEffects.PLAGUE.get(), 4800, 0));
                        if (e.getHealth() <= 0) {
                            e.getPersistentData().putBoolean("NecropolisEcho", true);
                            e.getPersistentData().putInt("NecropolisGen", generation + 1);
                            e.getPersistentData().putInt("NecropolisEchoLevel", echoLvl);
                        }
                    }
                }
            }
        }
    } catch (Throwable t) {
        System.out.println("[TitanForge-NewMagic-onKill] " + t.getClass().getName() + ": " + t.getMessage());
        t.printStackTrace();
    }}

    private void spawnBlackHole(World world, Vector3d pos, int ticks, double radius, int level) {
        net.minecraft.entity.item.ArmorStandEntity hole = net.minecraft.entity.EntityType.ARMOR_STAND.create(world);
        hole.setPosition(pos.x, pos.y, pos.z);
        hole.setInvisible(true);
        hole.setInvulnerable(true);
        hole.setNoGravity(true);
        hole.setSilent(true);
        hole.getPersistentData().putInt("IsBlackHole", ticks);
        hole.getPersistentData().putDouble("BlackHoleRadius", radius);
        hole.getPersistentData().putInt("BlackHoleDmg", level);
        world.addEntity(hole);
    }
}
