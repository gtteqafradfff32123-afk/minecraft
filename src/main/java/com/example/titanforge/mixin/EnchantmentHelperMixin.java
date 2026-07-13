package com.example.titanforge.mixin;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla clamps enchantment levels to 0..255 when reading them from NBT.
 * This removes the clamp so command-given enchantments can go to any int level.
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin {

    @Inject(method = "getEnchantmentLevel", at = @At("HEAD"), cancellable = true)
    private static void titanforge$uncapEnchantLevel(Enchantment enchantment, ItemStack stack,
                                                     CallbackInfoReturnable<Integer> cir) {
        if (stack.isEmpty()) {
            cir.setReturnValue(0);
            return;
        }
        ResourceLocation id = Registry.ENCHANTMENT.getKey(enchantment);
        ListNBT list = stack.getEnchantmentTagList();
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT nbt = list.getCompound(i);
            ResourceLocation entryId = ResourceLocation.tryCreate(nbt.getString("id"));
            if (entryId != null && entryId.equals(id)) {
                cir.setReturnValue(Math.max(0, nbt.getInt("lvl")));
                return;
            }
        }
        cir.setReturnValue(0);
    }
}
