package com.example.titanforge;

import net.minecraft.potion.Effect;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModEffects {
    public static final DeferredRegister<Effect> EFFECTS = DeferredRegister.create(ForgeRegistries.POTIONS, TitanForge.MOD_ID);

    public static final RegistryObject<Effect> FROSTBITTEN = EFFECTS.register("frostbitten", FrostbittenEffect::new);
    public static final RegistryObject<Effect> PLAGUE = EFFECTS.register("plague", PlagueEffect::new);
    public static final RegistryObject<Effect> BLOOD_FRENZY = EFFECTS.register("blood_frenzy", BloodFrenzyEffect::new);
    public static final RegistryObject<Effect> LIMINAL = EFFECTS.register("liminal", LiminalEffect::new);
    public static final RegistryObject<Effect> ZOMBIE_VIRUS = EFFECTS.register("zombie_virus", ZombieVirusEffect::new);

    public static final RegistryObject<Effect> ROT = EFFECTS.register("rot",
            () -> new net.minecraft.potion.Effect(net.minecraft.potion.EffectType.HARMFUL, 0x2b1b0e) {
                @Override
                public void performEffect(net.minecraft.entity.LivingEntity e, int amp) {
                    if (e.getHealth() > 1.0F)
                        e.attackEntityFrom(net.minecraft.util.DamageSource.WITHER, 1.0F);
                }
                @Override
                public boolean isReady(int duration, int amplifier) {
                    return duration % 60 == 0;
                }
            });
}
