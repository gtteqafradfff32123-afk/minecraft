package com.example.titanforge.backrooms;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;

public final class BackroomsDimension {
    public static final RegistryKey<World> WORLD = RegistryKey.getOrCreateKey(
        Registry.WORLD_KEY, new ResourceLocation("titanforge", "backrooms"));

    private BackroomsDimension() {}
}
