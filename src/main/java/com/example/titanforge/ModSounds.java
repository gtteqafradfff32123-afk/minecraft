package com.example.titanforge;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, TitanForge.MOD_ID);

    public static final RegistryObject<SoundEvent> BLOOD_FRENZY_BREATH = register("blood_frenzy_breath");
    public static final RegistryObject<SoundEvent> LIMINAL_RAGE = register("danger_around_the_corner");

    private static RegistryObject<SoundEvent> register(String name) {
        ResourceLocation id = new ResourceLocation(TitanForge.MOD_ID, name);
        return SOUNDS.register(name, () -> new SoundEvent(id));
    }

    private ModSounds() {}
}
