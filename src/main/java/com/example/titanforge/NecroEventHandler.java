package com.example.titanforge;

import com.example.titanforge.entities.StunZombieEntity;
import net.minecraft.block.*;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID)
public class NecroEventHandler {

    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        ItemStack bow = event.getBow();
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.ZEUS_VOLLEY.get(), bow) == 0) return;
        if (event.getPlayer().world.isRemote) return;
        if (event.getCharge() < 20) return;
        PlayerEntity player = event.getPlayer();
        if (player.getLookVec().y < 0.6) return;
        if (player.getCooldownTracker().hasCooldown(bow.getItem())) return;
        if (!player.abilities.isCreativeMode && countArrows(player) < 8) return;

        player.getCooldownTracker().setCooldown(bow.getItem(), 30 * 20);
        if (!player.abilities.isCreativeMode) consumeArrows(player, 8);
        bow.damageItem(25, player, p -> {});
        ZeusStormManager.begin((ServerPlayerEntity) player);
    }

    @SubscribeEvent
    public static void onCrit(CriticalHitEvent event) {
        if (!event.isVanillaCritical()) return;
        ItemStack sword = event.getPlayer().getHeldItemMainhand();
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.PSYCHOTIC_BREAK.get(), sword) == 0) return;
        PlayerEntity player = event.getPlayer();
        if (player.getCooldownTracker().hasCooldown(sword.getItem())) return;
        Entity target = event.getTarget();
        if (target instanceof net.minecraft.entity.boss.WitherEntity ||
            target instanceof net.minecraft.entity.boss.dragon.EnderDragonEntity) return;

        player.getCooldownTracker().setCooldown(sword.getItem(), 30 * 20);
        player.addPotionEffect(new EffectInstance(Effects.HUNGER, 100, 2));
        sword.damageItem(8, player, p -> {});

        if (target instanceof MobEntity) {
            ChaosAiManager.apply((MobEntity) target, 200);
        } else if (target instanceof ServerPlayerEntity) {
            target.attackEntityFrom(DamageSource.causePlayerDamage(player), 0.5F);
            ((ServerPlayerEntity) target).addPotionEffect(new EffectInstance(Effects.NAUSEA, 200, 0));
        }
    }

    @SubscribeEvent
    public static void onCritLiminal(CriticalHitEvent event) {
        if (!event.isVanillaCritical()) return;
        ItemStack sword = event.getPlayer().getHeldItemMainhand();
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.LIMINAL_SLIP.get(), sword) == 0) return;
        LivingEntity victim = (LivingEntity) event.getTarget();
        if (victim == null || victim.getHealth() > victim.getMaxHealth() * 0.5F) return;
        PlayerEntity owner = event.getPlayer();
        if (owner.getCooldownTracker().hasCooldown(sword.getItem())) return;

        owner.getCooldownTracker().setCooldown(sword.getItem(), 180 * 20);
        owner.setHealth(Math.max(1.0F, owner.getHealth() - 6.0F));
        sword.damageItem(30, owner, p -> {});
        LiminalManager.enter(victim, owner, 120 + owner.world.rand.nextInt(120));
    }

    @SubscribeEvent
    public static void onMembraneHurt(LivingHurtEvent event) {
        if (!(event.getEntityLiving() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getEntityLiving();
        ItemStack chest = player.getItemStackFromSlot(EquipmentSlotType.CHEST);
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.MEMBRANE_WEAVER.get(), chest) == 0) return;

        player.getCapability(MembraneCapability.CAP).ifPresent(m -> {
            float dmg = event.getAmount();
            float absorbed = dmg * 0.70F;
            event.setAmount(dmg * 0.30F);
            m.store(absorbed);
            chest.damageItem(2, player, p -> {});

            if (m.getStacks() >= 8 || m.getStored() >= 40F) m.startBurstTimer(15 * 20);
        });
    }

    @SubscribeEvent
    public static void onDissolverHurt(LivingHurtEvent event) {
        if (!(event.getSource().getTrueSource() instanceof PlayerEntity)) return;
        PlayerEntity player = (PlayerEntity) event.getSource().getTrueSource();
        ItemStack tool = player.getHeldItemMainhand();
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.TRUTH_DISSOLVER.get(), tool) == 0) return;
        if (!(event.getEntityLiving() instanceof LivingEntity)) return;
        LivingEntity victim = (LivingEntity) event.getEntityLiving();

        if (victim instanceof PlayerEntity) {
            stripRandomArmor((PlayerEntity) victim);
        } else {
            dropAllArmorBroken(victim);
            victim.addPotionEffect(new EffectInstance(Effects.WEAKNESS, 160, 2));
            victim.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 160, 2));
        }
        DissolveAbsorbTracker.mark(victim, player, 160);
        tool.damageItem(12, player, p -> {});
    }

    @SubscribeEvent
    public static void onDissolverRightClick(PlayerInteractEvent.RightClickBlock event) {
        PlayerEntity player = event.getPlayer();
        ItemStack tool = player.getHeldItemMainhand();
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.TRUTH_DISSOLVER.get(), tool) == 0) return;
        if (player.world.isRemote) return;
        ServerWorld world = (ServerWorld) player.world;
        BlockPos hit = event.getPos();

        for (BlockPos p : BlockPos.getAllInBoxMutable(hit.add(-1, -1, -1), hit.add(1, 1, 1))) {
            world.setBlockState(p, Blocks.BLACK_STAINED_GLASS.getDefaultState());
        }
        tool.getOrCreateTag().putLong("neMineLock", world.getGameTime() + 200);
    }

    @SubscribeEvent
    public static void onDoorBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!LiminalManager.isInside(event.getPlayer())) return;
        Block b = event.getWorld().getBlockState(event.getPos()).getBlock();
        if (b instanceof DoorBlock || b instanceof TrapDoorBlock || b instanceof FenceGateBlock)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBreakWall(net.minecraftforge.event.world.BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof net.minecraft.entity.player.ServerPlayerEntity)) return;
        if (!LiminalManager.isInside(event.getPlayer())) return;
        if (event.getState().getBlock() == Blocks.BLACK_CONCRETE)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onNecroticUndertowRightClick(PlayerInteractEvent.RightClickItem event) {
        PlayerEntity player = event.getPlayer();
        if (player.world.isRemote) return;
        ItemStack stack = event.getItemStack();
        if (EnchantmentHelper.getEnchantmentLevel(ModEnchantments.NECROTIC_UNDERTOW.get(), stack) == 0) return;
        if (player.getCooldownTracker().hasCooldown(stack.getItem())) return;

        event.setCanceled(true);
        player.getCooldownTracker().setCooldown(stack.getItem(), 45 * 20);
        player.setHealth(Math.max(1.0F, player.getHealth() - 4.0F));
        stack.damageItem(15, player, p -> p.sendBreakAnimation(Hand.MAIN_HAND));

        ServerWorld world = (ServerWorld) player.world;

        // Wave visual
        world.playSound(null, player.getPosition(), SoundEvents.ENTITY_WITHER_SPAWN,
            SoundCategory.HOSTILE, 0.8F, 0.4F);
        world.spawnParticle(ParticleTypes.SOUL, player.getPosX(), player.getPosY() + 0.5, player.getPosZ(),
            60, 4.0, 0.5, 4.0, 0.1);
        world.spawnParticle(ParticleTypes.LARGE_SMOKE, player.getPosX(), player.getPosY() + 0.2, player.getPosZ(),
            40, 4.0, 0.1, 4.0, 0.02);

        AxisAlignedBB box = new AxisAlignedBB(player.getPosition()).grow(10.0D);
        List<LivingEntity> targets = world.getEntitiesWithinAABB(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        com.example.titanforge.TitanForge.LOGGER.info("[NecroUndertow] activated by {}, {} targets in range",
            player.getName().getString(), targets.size());

        for (LivingEntity target : targets) {
            target.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 250, 4));
            NecroChokeTracker.mark(target, (ServerPlayerEntity) player, 250);
            target.attackEntityFrom(ModDamageSources.NECROTIC_UNDERTOW, 8.0F);
            com.example.titanforge.TitanForge.LOGGER.info("[NecroUndertow] marked {} as choke target", target.getName().getString());
        }
    }

    @SubscribeEvent
    public static void onNecroticDeath(LivingDeathEvent event) {
        if (event.getEntityLiving().world.isRemote) return;
        ServerWorld world = (ServerWorld) event.getEntityLiving().world;
        NecroChokeTracker.onDeath(event.getEntityLiving(), world);
        DissolveAbsorbTracker.onDeath(event.getEntityLiving());
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.world instanceof ServerWorld)) return;
        ServerWorld world = (ServerWorld) event.world;

        NecroChokeTracker.tick(world);
        ZeusStormManager.tick(world);
        ChaosAiManager.tick();
        DissolveAbsorbTracker.tick();
    }

    public static List<LivingEntity> entitiesInCone(LivingEntity origin, double range, double angleDeg) {
        Vector3d look = origin.getLook(1.0F).normalize();
        AxisAlignedBB box = new AxisAlignedBB(origin.getPosition()).grow(range);
        List<LivingEntity> inRange = origin.world.getEntitiesWithinAABB(LivingEntity.class, box,
                e -> e != origin && e.isAlive() && e.getDistance(origin) <= range);
        List<LivingEntity> cone = new ArrayList<>();
        double dotThreshold = Math.cos(Math.toRadians(angleDeg / 2.0));
        for (LivingEntity e : inRange) {
            Vector3d to = e.getPositionVec().subtract(origin.getPositionVec()).normalize();
            if (look.dotProduct(to) >= dotThreshold)
                cone.add(e);
        }
        return cone;
    }

    private static int countArrows(PlayerEntity player) {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof ArrowItem) count += stack.getCount();
        }
        return count;
    }

    private static void consumeArrows(PlayerEntity player, int amount) {
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack.getItem() instanceof ArrowItem) {
                int taken = Math.min(stack.getCount(), amount);
                stack.shrink(taken);
                amount -= taken;
                if (amount <= 0) break;
            }
        }
    }

    private static void stripRandomArmor(PlayerEntity player) {
        List<EquipmentSlotType> slots = Arrays.asList(
                EquipmentSlotType.HEAD, EquipmentSlotType.CHEST,
                EquipmentSlotType.LEGS, EquipmentSlotType.FEET);
        Collections.shuffle(slots, player.world.rand);
        for (EquipmentSlotType slot : slots) {
            ItemStack armor = player.getItemStackFromSlot(slot);
            if (!armor.isEmpty()) {
                armor.damageItem((int) (armor.getMaxDamage() * 0.5F), player, p -> {});
                if (armor.getDamage() >= armor.getMaxDamage()) {
                    player.setItemStackToSlot(slot, ItemStack.EMPTY);
                }
                return;
            }
        }
    }

    private static void dropAllArmorBroken(LivingEntity mob) {
        for (EquipmentSlotType slot : EquipmentSlotType.values()) {
            ItemStack armor = mob.getItemStackFromSlot(slot);
            if (armor.isEmpty()) continue;
            armor.setDamage(armor.getMaxDamage() - 1);
            mob.entityDropItem(armor, 0.0F);
            mob.setItemStackToSlot(slot, ItemStack.EMPTY);
        }
    }
}
