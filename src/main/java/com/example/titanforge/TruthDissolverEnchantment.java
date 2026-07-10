package com.example.titanforge;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;

public class TruthDissolverEnchantment extends Enchantment {
    public TruthDissolverEnchantment() {
        super(Rarity.RARE, EnchantmentType.DIGGER, new EquipmentSlotType[]{EquipmentSlotType.MAINHAND});
    }

    @Override
    public int getMaxLevel() { return 1; }
}
