package com.example.titanforge;

import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTION_TYPES, TitanForge.MOD_ID);

    public static final RegistryObject<Potion> PLAGUE_POTION = POTIONS.register("plague",
        () -> new Potion("plague", new EffectInstance(ModEffects.PLAGUE.get(), 4800, 0)));

    public static final RegistryObject<Potion> LIMINAL_POTION = POTIONS.register("liminal",
        () -> new Potion("liminal", new EffectInstance(ModEffects.LIMINAL.get(), 40, 0)));
}
