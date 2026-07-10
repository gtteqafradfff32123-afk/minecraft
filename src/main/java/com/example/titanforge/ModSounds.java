package com.example.titanforge;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TitanForge.MOD_ID);

    public static final RegistryObject<SoundEvent> BLOOD_FRENZY_BREATH = SOUNDS.register("blood_frenzy_breath",
        () -> new SoundEvent(new ResourceLocation(TitanForge.MOD_ID, "blood_frenzy_breath")));

    public static final RegistryObject<SoundEvent> LIMINAL_RAGE = SOUNDS.register("danger_around_the_corner",
        () -> new SoundEvent(new ResourceLocation(TitanForge.MOD_ID, "danger_around_the_corner")));
}
