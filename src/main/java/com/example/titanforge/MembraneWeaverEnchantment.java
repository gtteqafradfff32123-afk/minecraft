package com.example.titanforge;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;

public class MembraneWeaverEnchantment extends Enchantment {
    public MembraneWeaverEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentType.ARMOR_CHEST, new EquipmentSlotType[]{EquipmentSlotType.CHEST});
    }

    @Override
    public int getMaxLevel() { return 1; }

    @Override
    public boolean canGenerateInLoot() {
        return false;
    }

    @Override
    public boolean canVillagerTrade() {
        return false;
    }
}
