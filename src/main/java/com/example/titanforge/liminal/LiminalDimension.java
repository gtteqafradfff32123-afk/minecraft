package com.example.titanforge.liminal;

import com.example.titanforge.TitanForge;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.server.MinecraftServer;

public class LiminalDimension {
    public static final RegistryKey<World> LIMINAL_WORLD =
        RegistryKey.getOrCreateKey(Registry.WORLD_KEY,
            new net.minecraft.util.ResourceLocation(TitanForge.MOD_ID, "liminal"));

    public static final RegistryKey<DimensionType> LIMINAL_TYPE =
        RegistryKey.getOrCreateKey(Registry.DIMENSION_TYPE_KEY,
            new net.minecraft.util.ResourceLocation(TitanForge.MOD_ID, "liminal_type"));

    public static ServerWorld get(MinecraftServer server) {
        ServerWorld world = server.getWorld(LIMINAL_WORLD);
        if (world == null) {
            TitanForge.LOGGER.warn("[TitanForge] liminal dimension not registered - check datapack JSON. Need a new world.");
            return null;
        }
        return world;
    }

    public static void syncSeedWithOverworld(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        long owSeed = overworld.getSeed();
        TitanForge.LOGGER.info("[TitanForge] liminal seed synced with overworld: {}", owSeed);
    }
}
