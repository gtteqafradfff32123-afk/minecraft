package com.example.titanforge;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.EndermanEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.monster.ZombieVillagerEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.horse.ZombieHorseEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Зомби Вирус: удары накапливают инфекцию, болезнь идёт по стадиям,
 * умершие встают гнилыми версиями себя, NPC из CustomNPCs на стадии 3
 * мгновенно заменяются зомби с их именем и снаряжением.
 */
public final class ZombieVirusHandler {
    // Ключи NBT
    private static final String K_INFECTION = "TF_VirusInfection";
    private static final String K_STAGE = "TF_VirusStage";
    private static final String K_TIMER = "TF_VirusStageTimer";
    private static final String K_OWNER = "TF_VirusOwner";
    private static final String K_LEVEL = "TF_VirusLevel";
    private static final String K_SERVANT = "TF_VirusServant";
    private static final String K_ZOMBIFIED = "TF_Zombified";
    private static final String K_BORN = "TF_ZombifiedTime";

    private static final int STAGE_TICKS = 160;      // 8 секунд на стадию
    private static final int SERVANT_LIFETIME = 2400; // 2 минуты

    /** Мобы, у которых есть своя зомби-текстура (встают той же тушкой, но гнилой). */
    private static final Set<EntityType<?>> TEXTURED = new HashSet<>();
    static {
        TEXTURED.add(EntityType.COW);
        TEXTURED.add(EntityType.PIG);
        TEXTURED.add(EntityType.SHEEP);
        TEXTURED.add(EntityType.CHICKEN);
        TEXTURED.add(EntityType.WOLF);
        TEXTURED.add(EntityType.FOX);
        TEXTURED.add(EntityType.RABBIT);
        TEXTURED.add(EntityType.SPIDER);
    }

    // ==================== ЗАРАЖЕНИЕ ПРИ УДАРЕ ====================

    @SubscribeEvent
    public void onAttack(LivingHurtEvent event) {
        try {
            Entity trueSrc = event.getSource().getTrueSource();
            if (!(trueSrc instanceof LivingEntity)) return;
            LivingEntity attacker = (LivingEntity) trueSrc;
            if (attacker.world.isRemote) return;
            if (event.getSource().isExplosion() || event.getSource().isMagicDamage()) return;

            ItemStack weapon = attacker.getHeldItemMainhand();
            int lvl = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.ZOMBIE_VIRUS.get(), weapon);
            if (lvl <= 0) return;

            LivingEntity target = event.getEntityLiving();
            UUID ownerId = attacker.getUniqueID();
            ServerWorld sw = (ServerWorld) attacker.world;

            // Свои слуги неприкосновенны
            if (isServantOf(target, ownerId)) {
                event.setCanceled(true);
                return;
            }

            // Нежить не болеет — вирус её ПОДЧИНЯЕТ (шанс 15% за уровень)
            if (target.isEntityUndead()) {
                float chance = Math.min(0.15F * lvl, 0.9F);
                if (attacker.getRNG().nextFloat() < chance && !target.getPersistentData().getBoolean(K_SERVANT)) {
                    makeServant(target, ownerId, lvl);
                    sw.spawnParticle(ParticleTypes.HAPPY_VILLAGER, target.getPosX(), target.getPosY() + 1.0, target.getPosZ(), 12, 0.4, 0.5, 0.4, 0.02);
                    sw.playSound(null, target.getPosX(), target.getPosY(), target.getPosZ(), SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 0.9F, 0.6F);
                }
                return;
            }

            // Накопление инфекции
            int base = lvl == 1 ? 20 : lvl == 2 ? 30 : lvl == 3 ? 45 : 45 + (lvl - 3) * 10;
            if (attacker instanceof PlayerEntity) {
                PlayerEntity p = (PlayerEntity) attacker;
                boolean crit = p.fallDistance > 0.0F && !p.isOnGround() && !p.isInWater() && !p.isOnLadder();
                if (crit) base = (int) (base * 1.5F);
            }
            // Сопротивление: эндермены и големы заражаются вдвое медленнее
            if (target instanceof EndermanEntity || target instanceof IronGolemEntity) base /= 2;

            addInfection(target, base, ownerId, lvl);

            if (!target.world.isRemote) {
                sw.spawnParticle(ParticleTypes.SNEEZE, target.getPosX(), target.getPosY() + target.getHeight() * 0.6, target.getPosZ(), 6, 0.3, 0.3, 0.3, 0.02);
            }

            // Риск носителя: 5% (10% на 3+ уровне) уколоться самому
            if (attacker instanceof PlayerEntity && getStage(attacker) == 0) {
                float selfChance = lvl >= 3 ? 0.10F : 0.05F;
                if (attacker.getRNG().nextFloat() < selfChance) {
                    attacker.getPersistentData().putUniqueId(K_OWNER, ownerId);
                    attacker.getPersistentData().putInt(K_LEVEL, lvl);
                    setStage(attacker, 1);
                }
            }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-ZombieVirus-onAttack] Error in event", t);
        }
    }

    private static void addInfection(LivingEntity target, int amount, UUID owner, int lvl) {
        if (target.isEntityUndead() || target.getPersistentData().getBoolean(K_SERVANT)) return;
        CompoundNBT data = target.getPersistentData();
        if (data.getInt(K_STAGE) > 0) return; // уже болеет
        int infection = data.getInt(K_INFECTION) + amount;
        data.putInt(K_INFECTION, infection);
        data.putUniqueId(K_OWNER, owner);
        data.putInt(K_LEVEL, lvl);
        if (infection >= 100) setStage(target, 1);
    }

    private static void setStage(LivingEntity e, int stage) {
        CompoundNBT data = e.getPersistentData();
        data.putInt(K_STAGE, stage);
        data.putInt(K_TIMER, 0);
        if (e.world.isRemote) return;
        // Видимый эффект с иконкой: уровень = стадия
        e.removePotionEffect(ModEffects.ZOMBIE_VIRUS.get());
        e.addPotionEffect(new EffectInstance(ModEffects.ZOMBIE_VIRUS.get(), 1000000, stage - 1, false, false, true));
        if (e instanceof PlayerEntity) {
            String msg = stage == 1 ? "§2Вы чувствуете странное недомогание..."
                    : stage == 2 ? "§2Лихорадка усиливается. Нужно лекарство, быстро!"
                    : "§4Некроз. Лекарства уже не помогут.";
            ((PlayerEntity) e).sendStatusMessage(new StringTextComponent(msg), true);
        }
        e.world.playSound(null, e.getPosX(), e.getPosY(), e.getPosZ(),
                SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 0.5F, stage == 3 ? 0.5F : 1.3F);
    }

    private static int getStage(LivingEntity e) {
        return e.getPersistentData().getInt(K_STAGE);
    }

    private static boolean isServantOf(LivingEntity e, UUID owner) {
        CompoundNBT data = e.getPersistentData();
        return data.getBoolean(K_SERVANT) && data.hasUniqueId(K_OWNER) && owner.equals(data.getUniqueId(K_OWNER));
    }

    private static void makeServant(LivingEntity e, UUID owner, int lvl) {
        CompoundNBT data = e.getPersistentData();
        data.putBoolean(K_SERVANT, true);
        data.putUniqueId(K_OWNER, owner);
        data.putInt(K_LEVEL, lvl);
        data.putLong(K_BORN, e.world.getGameTime());
        if (e instanceof MobEntity) ((MobEntity) e).enablePersistence();
    }

    // ==================== ТЕЧЕНИЕ БОЛЕЗНИ + СЛУГИ ====================

    @SubscribeEvent
    public void onUpdate(LivingEvent.LivingUpdateEvent event) {
        try {
            LivingEntity e = event.getEntityLiving();
            if (e.world.isRemote) return;
            CompoundNBT data = e.getPersistentData();

            // --- Логика слуги ---
            if (data.getBoolean(K_SERVANT)) {
                tickServant(e, data);
                return;
            }

            // --- Логика болезни ---
            int stage = data.getInt(K_STAGE);

            // Заражение через /effect give titanforge:zombie_virus
            if (stage <= 0) {
                EffectInstance manual = e.getActivePotionEffect(ModEffects.ZOMBIE_VIRUS.get());
                if (manual != null && !e.isEntityUndead()) {
                    data.putUniqueId(K_OWNER, e.getUniqueID());
                    data.putInt(K_LEVEL, 1);
                    setStage(e, Math.min(3, manual.getAmplifier() + 1));
                }
                return;
            }
            ServerWorld sw = (ServerWorld) e.world;

            // Эффект-индикатор не должен слетать (молоко и т.п. не лечат сами по себе)
            if (e.ticksExisted % 100 == 0 && !e.isPotionActive(ModEffects.ZOMBIE_VIRUS.get())) {
                e.addPotionEffect(new EffectInstance(ModEffects.ZOMBIE_VIRUS.get(), 1000000, stage - 1, false, false, true));
            }

            int timer = data.getInt(K_TIMER) + 1;
            data.putInt(K_TIMER, timer);

            // Переход на следующую стадию
            if (stage < 3 && timer >= STAGE_TICKS) {
                int next = stage + 1;
                // NPC из CustomNPCs на стадии 3 не умирает — он ПРЕВРАЩАЕТСЯ
                if (next == 3 && CustomNpcChaosBridge.isCustomNpc(e)) {
                    convertNpcToZombie(e);
                    return;
                }
                setStage(e, next);
                stage = next;
            }

            // Эффекты стадий
            if (stage == 1) {
                if (timer % 100 == 0) {
                    sw.spawnParticle(ParticleTypes.SNEEZE, e.getPosX(), e.getPosY() + e.getHeight() * 0.7, e.getPosZ(), 3, 0.2, 0.2, 0.2, 0.01);
                    sw.playSound(null, e.getPosX(), e.getPosY(), e.getPosZ(), SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.NEUTRAL, 0.25F, 1.5F);
                }
            } else if (stage == 2) {
                if (timer % 40 == 0) {
                    e.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 60, 0));
                    e.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 60, 0));
                }
                if (timer % 80 == 0) hurtByVirus(e, 1.0F);
            } else {
                if (timer % 40 == 0) {
                    e.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 60, 1));
                    e.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 60, 1));
                    if (e instanceof PlayerEntity) e.addPotionEffect(new EffectInstance(Effects.HUNGER, 60, 2));
                    hurtByVirus(e, 1.0F);
                    sw.spawnParticle(ParticleTypes.SNEEZE, e.getPosX(), e.getPosY() + e.getHeight() * 0.5, e.getPosZ(), 8, 0.3, 0.4, 0.3, 0.03);
                }
            }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-ZombieVirus-onUpdate] Error in event", t);
        }
    }

    private static void hurtByVirus(LivingEntity e, float dmg) {
        // NPC не должен умереть от тика вируса — вместо смерти мгновенное превращение
        if (CustomNpcChaosBridge.isCustomNpc(e) && e.getHealth() - dmg <= 0.0F) {
            convertNpcToZombie(e);
            return;
        }
        e.attackEntityFrom(ModDamageSources.ZOMBIE_VIRUS, dmg);
    }

    private void tickServant(LivingEntity e, CompoundNBT data) {
        ServerWorld sw = (ServerWorld) e.world;
        long now = e.world.getGameTime();

        // Срок жизни истёк — рассыпается в костную муку
        if (now - data.getLong(K_BORN) > SERVANT_LIFETIME) {
            sw.spawnParticle(ParticleTypes.POOF, e.getPosX(), e.getPosY() + 0.5, e.getPosZ(), 15, 0.3, 0.4, 0.3, 0.02);
            e.entityDropItem(new ItemStack(Items.BONE_MEAL, 1 + e.getRNG().nextInt(2)));
            e.remove();
            return;
        }

        // Гнилая аура
        if (e.ticksExisted % 30 == 0) {
            sw.spawnParticle(ParticleTypes.SNEEZE, e.getPosX(), e.getPosY() + e.getHeight() * 0.6, e.getPosZ(), 2, 0.2, 0.3, 0.2, 0.01);
        }

        // Боевой импульс: идём к ближайшему монстру и бьём (заражая)
        if (!(e instanceof MobEntity)) return;
        MobEntity mob = (MobEntity) e;
        UUID owner = data.hasUniqueId(K_OWNER) ? data.getUniqueId(K_OWNER) : null;

        if (e.ticksExisted % 10 == 0) {
            LivingEntity best = null;
            double bestDist = 12.0 * 12.0;
            for (MonsterEntity m : sw.getEntitiesWithinAABB(MonsterEntity.class, e.getBoundingBox().grow(12.0))) {
                if (m == e || m.getPersistentData().getBoolean(K_SERVANT)) continue;
                double d = e.getDistanceSq(m);
                if (d < bestDist) { bestDist = d; best = m; }
            }
            if (best != null) {
                mob.setAttackTarget(best);
                mob.getNavigator().tryMoveToEntityLiving(best, 1.2D);
            }
        }

        LivingEntity target = mob.getAttackTarget();
        if (target != null && target.isAlive() && e.ticksExisted % 20 == 0 && mob.getDistanceSq(target) <= 6.25D) {
            target.hurtResistantTime = 0;
            target.attackEntityFrom(DamageSource.causeMobDamage(mob), 3.0F);
            mob.swingArm(net.minecraft.util.Hand.MAIN_HAND);
            if (owner != null) addInfection(target, 10, owner, Math.max(1, data.getInt(K_LEVEL)));
        }
    }

    // Слуги и подчинённая нежить не трогают владельца и его слуг
    @SubscribeEvent
    public void onSetTarget(LivingSetAttackTargetEvent event) {
        try {
            if (!(event.getEntityLiving() instanceof MobEntity)) return;
            MobEntity mob = (MobEntity) event.getEntityLiving();
            if (!mob.getPersistentData().getBoolean(K_SERVANT)) return;
            LivingEntity target = event.getTarget();
            if (target == null) return;
            UUID owner = mob.getPersistentData().hasUniqueId(K_OWNER) ? mob.getPersistentData().getUniqueId(K_OWNER) : null;
            if (owner != null && (owner.equals(target.getUniqueID()) || isServantOf(target, owner))) {
                mob.setAttackTarget(null);
            }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-ZombieVirus-onSetTarget] Error in event", t);
        }
    }

    // ==================== СМЕРТЬ И ВОССТАНИЕ ====================

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        try {
            LivingEntity dead = event.getEntityLiving();
            if (dead.world.isRemote) return;
            CompoundNBT data = dead.getPersistentData();
            int stage = data.getInt(K_STAGE);
            boolean byVirus = event.getSource() == ModDamageSources.ZOMBIE_VIRUS;
            if (stage < 2 && !(byVirus && stage >= 1)) return;

            ServerWorld sw = (ServerWorld) dead.world;
            UUID owner = data.hasUniqueId(K_OWNER) ? data.getUniqueId(K_OWNER) : null;
            int lvl = Math.max(1, data.getInt(K_LEVEL));

            // NPC: удалить и заменить зомби (обычно превращается ещё на стадии 3, это подстраховка)
            if (CustomNpcChaosBridge.isCustomNpc(dead)) {
                convertNpcToZombie(dead);
                return;
            }

            // Игрок: встаёт зомби с его именем и бронёй
            if (dead instanceof PlayerEntity) {
                ZombieEntity z = EntityType.ZOMBIE.create(sw);
                if (z != null) {
                    z.setLocationAndAngles(dead.getPosX(), dead.getPosY(), dead.getPosZ(), dead.rotationYaw, 0);
                    z.setCustomName(new StringTextComponent("§2" + dead.getDisplayName().getString()));
                    z.setCustomNameVisible(true);
                    copyEquipment(dead, z);
                    z.enablePersistence();
                    sw.addEntity(z);
                    riseEffects(sw, z);
                }
                return;
            }

            // Моб: встаёт гнилой версией себя (лимит слуг: 2 за уровень)
            if (owner == null) return;
            int limit = Math.min(2 * lvl, 20);
            int count = 0;
            for (LivingEntity s : sw.getEntitiesWithinAABB(LivingEntity.class, dead.getBoundingBox().grow(64.0))) {
                if (isServantOf(s, owner)) count++;
            }
            if (count >= limit) return;

            LivingEntity risen = createRisen(sw, dead);
            if (risen == null) return;
            risen.setLocationAndAngles(dead.getPosX(), dead.getPosY(), dead.getPosZ(), dead.rotationYaw, 0);
            risen.setHealth(Math.max(1.0F, risen.getMaxHealth() * 0.6F));
            risen.setCustomName(new StringTextComponent("§2" + dead.getType().getName().getString() + "-зомби"));
            risen.setCustomNameVisible(true);
            makeServant(risen, owner, lvl);
            risen.getPersistentData().putBoolean(K_ZOMBIFIED, true);
            sw.addEntity(risen);
            riseEffects(sw, risen);
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-ZombieVirus-onDeath] Error in event", t);
        }
    }

    /** Гнилая версия моба: с текстурой — та же тушка, лошадь/житель — родные зомби-версии, остальные — зомби. */
    private static LivingEntity createRisen(ServerWorld sw, LivingEntity dead) {
        EntityType<?> type = dead.getType();
        if (TEXTURED.contains(type)) {
            Entity e = type.create(sw);
            return e instanceof LivingEntity ? (LivingEntity) e : null;
        }
        if (type == EntityType.HORSE || type == EntityType.DONKEY || type == EntityType.MULE) {
            ZombieHorseEntity zh = EntityType.ZOMBIE_HORSE.create(sw);
            return zh;
        }
        if (dead instanceof VillagerEntity) {
            ZombieVillagerEntity zv = EntityType.ZOMBIE_VILLAGER.create(sw);
            if (zv != null) zv.setVillagerData(((VillagerEntity) dead).getVillagerData());
            return zv;
        }
        return EntityType.ZOMBIE.create(sw);
    }

    /** NPC не умирает: удаляется, и в ту же секунду на его месте встаёт зомби с его именем и вещами. */
    private static void convertNpcToZombie(LivingEntity npc) {
        if (npc.world.isRemote || !npc.isAlive()) return;
        ServerWorld sw = (ServerWorld) npc.world;

        ZombieEntity zombie = EntityType.ZOMBIE.create(sw);
        if (zombie == null) return;
        zombie.setLocationAndAngles(npc.getPosX(), npc.getPosY(), npc.getPosZ(), npc.rotationYaw, npc.rotationPitch);
        zombie.setCustomName(npc.getDisplayName());
        zombie.setCustomNameVisible(true);
        copyEquipment(npc, zombie);
        zombie.enablePersistence();

        npc.remove(); // без анимации смерти — просто исчез
        sw.addEntity(zombie);
        riseEffects(sw, zombie);
        sw.playSound(null, zombie.getPosX(), zombie.getPosY(), zombie.getPosZ(),
                SoundEvents.ENTITY_ZOMBIE_VILLAGER_CONVERTED, SoundCategory.HOSTILE, 1.0F, 0.8F);
    }

    private static void copyEquipment(LivingEntity from, MobEntity to) {
        for (EquipmentSlotType slot : EquipmentSlotType.values()) {
            ItemStack stack = from.getItemStackFromSlot(slot);
            if (!stack.isEmpty()) {
                to.setItemStackToSlot(slot, stack.copy());
                to.setDropChance(slot, 0.0F); // не дюпаем вещи при смерти зомби
            }
        }
    }

    private static void riseEffects(ServerWorld sw, LivingEntity risen) {
        sw.spawnParticle(ParticleTypes.CLOUD, risen.getPosX(), risen.getPosY() + 0.8, risen.getPosZ(), 20, 0.4, 0.6, 0.4, 0.03);
        sw.spawnParticle(ParticleTypes.SNEEZE, risen.getPosX(), risen.getPosY() + 0.5, risen.getPosZ(), 25, 0.4, 0.5, 0.4, 0.05);
        sw.playSound(null, risen.getPosX(), risen.getPosY(), risen.getPosZ(),
                SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.HOSTILE, 1.0F, 0.7F);
    }

    // ==================== ЛЕЧЕНИЕ ====================

    @SubscribeEvent
    public void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        try {
            LivingEntity e = event.getEntityLiving();
            if (e.world.isRemote) return;
            int stage = getStage(e);
            if (stage <= 0 || stage >= 3) return; // стадия 3 неизлечима

            ItemStack item = event.getItem();
            boolean cured = false;
            if (stage == 1 && (item.getItem() == Items.MILK_BUCKET || item.getItem() == Items.GOLDEN_APPLE)) {
                cured = true;
            } else if (stage == 2 && item.getItem() == Items.GOLDEN_APPLE && e.isPotionActive(Effects.REGENERATION)) {
                cured = true;
            }
            if (item.getItem() == Items.ENCHANTED_GOLDEN_APPLE) cured = true; // зачарованное лечит обе стадии

            if (cured) {
                CompoundNBT data = e.getPersistentData();
                data.remove(K_STAGE);
                data.remove(K_TIMER);
                data.remove(K_INFECTION);
                e.removePotionEffect(ModEffects.ZOMBIE_VIRUS.get());
                e.world.playSound(null, e.getPosX(), e.getPosY(), e.getPosZ(),
                        SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.NEUTRAL, 0.8F, 1.2F);
                if (e instanceof PlayerEntity) {
                    ((PlayerEntity) e).sendStatusMessage(new StringTextComponent("§aВирус побеждён!"), true);
                }
            }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-ZombieVirus-onUseItemFinish] Error in event", t);
        }
    }

    // ==================== СИНХРОНИЗАЦИЯ ТЕКСТУР НА КЛИЕНТ ====================

    @SubscribeEvent
    public void onStartTracking(PlayerEvent.StartTracking event) {
        try {
            if (!(event.getPlayer() instanceof ServerPlayerEntity)) return;
            Entity target = event.getTarget();
            if (target.getPersistentData().getBoolean(K_ZOMBIFIED)) {
                NetworkHandler.INSTANCE.send(
                        PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer()),
                        new ZombifiedSyncPacket(target.getEntityId()));
            }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-ZombieVirus-onStartTracking] Error in event", t);
        }
    }
}
