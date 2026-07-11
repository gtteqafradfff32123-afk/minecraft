package com.example.titanforge.backrooms;

import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = com.example.titanforge.TitanForge.MOD_ID)
public final class BackroomsDimension {
    public static final RegistryKey<World> WORLD =
            RegistryKey.getOrCreateKey(
                    Registry.WORLD_KEY,
                    new ResourceLocation(
                            com.example.titanforge.TitanForge.MOD_ID,
                            "backrooms"));

    private BackroomsDimension() {}

    public static ServerWorld get(net.minecraft.server.MinecraftServer server) {
        ServerWorld world = server.getWorld(WORLD);
        if (world == null) {
            com.example.titanforge.TitanForge.LOGGER.warn(
                    "Backrooms dimension not available");
        }
        return world;
    }
}
