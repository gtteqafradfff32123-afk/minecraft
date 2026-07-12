package com.example.titanforge;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;

public final class UnstableEdgeEnchantment extends Enchantment {
    public UnstableEdgeEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentType.WEAPON,
                new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
    }

    @Override public int getMinEnchantability(int level) { return 18 + level * 12; }
    @Override public int getMaxEnchantability(int level) { return getMinEnchantability(level) + 24; }
    @Override public int getMaxLevel() { return 3; }

    @Override
    public boolean canGenerateInLoot() {
        return false;
    }

    @Override
    public boolean canVillagerTrade() {
        return false;
    }
}
