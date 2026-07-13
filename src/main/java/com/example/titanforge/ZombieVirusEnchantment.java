package com.example.titanforge;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;

public final class ZombieVirusEnchantment extends Enchantment {
    public ZombieVirusEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentType.WEAPON,
                new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
    }

    @Override public int getMinEnchantability(int level) { return 25 + level * 15; }
    @Override public int getMaxEnchantability(int level) { return getMinEnchantability(level) + 30; }
    @Override public int getMaxLevel() { return 3; }

    @Override
    public boolean canGenerateInLoot() {
        return false;
    }

    @Override
    public boolean canVillagerTrade() {
        return false;
    }

    @Override
    protected boolean canApplyTogether(Enchantment other) {
        // Холод убивает вирус, огонь стерилизует
        if (other == ModEnchantments.FROSTBITE.get()) return false;
        if (other == ModEnchantments.SOLAR_FLARE.get()) return false;
        return super.canApplyTogether(other);
    }
}
