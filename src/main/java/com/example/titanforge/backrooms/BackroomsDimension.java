package com.example.titanforge.backrooms;

import com.example.titanforge.TitanForge;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

public final class BackroomsDimension {
    public static final RegistryKey<World> WORLD = RegistryKey.getOrCreateKey(
        Registry.WORLD_KEY, new ResourceLocation("titanforge", "backrooms"));

    public static final RegistryKey<DimensionType> TYPE = RegistryKey.getOrCreateKey(
        Registry.DIMENSION_TYPE_KEY, new ResourceLocation("titanforge", "backrooms_type"));

    private BackroomsDimension() {}

    public static ServerWorld get(MinecraftServer server) {
        ServerWorld world = server.getWorld(WORLD);
        if (world == null) {
            TitanForge.LOGGER.warn("[backrooms] dimension not registered - check datapack JSONs. Need a new world.");
            return null;
        }
        return world;
    }
}
