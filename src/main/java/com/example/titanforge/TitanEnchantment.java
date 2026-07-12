package com.example.titanforge;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.AxeItem;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import net.minecraft.item.SwordItem;

public class TitanEnchantment extends Enchantment {
    private final int maxLevel;
    private final EnchantmentType type;

    public TitanEnchantment(Rarity rarity, EnchantmentType type, int maxLevel) {
        super(rarity, type, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
        this.maxLevel = maxLevel;
        this.type = type;
    }

    @Override
    public int getMaxLevel() {
        return this.maxLevel;
    }

    @Override
    public boolean canApply(ItemStack stack) {
        if (this.type == EnchantmentType.WEAPON) {
            if (stack.getItem() instanceof SwordItem) return true;
            return !stack.getAttributeModifiers(EquipmentSlotType.MAINHAND).get(Attributes.ATTACK_DAMAGE).isEmpty();
        }
        if (this.type == EnchantmentType.DIGGER) {
            return stack.getItem() instanceof PickaxeItem
                || stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof ShovelItem;
        }
        if (this.type == EnchantmentType.BOW) {
            return stack.getItem() instanceof BowItem;
        }
        if (this.type == EnchantmentType.CROSSBOW) {
            return stack.getItem() instanceof CrossbowItem;
        }
        if (this.type == ModEnchantments.BOW_AND_CROSSBOW) {
            return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
        }
        if (this.type == ModEnchantments.SHIELD) {
            return stack.getItem() == Items.SHIELD;
        }
        return super.canApply(stack);
    }

    @Override
    protected boolean canApplyTogether(Enchantment ench) {
        if (ench instanceof TitanEnchantment) {
            return true;
        }
        return super.canApplyTogether(ench);
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isAllowedOnBooks() {
        return false;
    }

    @Override
    public boolean canGenerateInLoot() {
        return false;
    }

    @Override
    public boolean canVillagerTrade() {
        return false;
    }
}
