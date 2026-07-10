package com.example.titanforge;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.world.World;

public class SyringeItem extends Item {

    public SyringeItem(Properties properties) {
        super(properties);
    }

    @Override
    public ActionResultType itemInteractionForEntity(ItemStack stack, PlayerEntity player, LivingEntity target, Hand hand) {
        if (target.isPotionActive(ModEffects.PLAGUE.get())) {
            if (!player.world.isRemote) {
                target.removePotionEffect(ModEffects.PLAGUE.get());
                target.getPersistentData().remove("HasPlague");
                player.world.playSound(null, target.getPosX(), target.getPosY(), target.getPosZ(), SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 1.0F, 1.0F);
                if (!player.abilities.isCreativeMode) {
                    stack.shrink(1);
                }
            }
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.PASS;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getHeldItem(hand);

        if (player.isPotionActive(ModEffects.PLAGUE.get())) {
            if (!world.isRemote) {
                player.removePotionEffect(ModEffects.PLAGUE.get());
                player.getPersistentData().remove("HasPlague");
                world.playSound(null, player.getPosX(), player.getPosY(), player.getPosZ(), SoundEvents.ENTITY_PLAYER_SPLASH, SoundCategory.PLAYERS, 1.0F, 1.0F);
                if (!player.abilities.isCreativeMode) {
                    stack.shrink(1);
                }
            }
            return new ActionResult<>(ActionResultType.SUCCESS, stack);
        }

        return new ActionResult<>(ActionResultType.PASS, stack);
    }
}
