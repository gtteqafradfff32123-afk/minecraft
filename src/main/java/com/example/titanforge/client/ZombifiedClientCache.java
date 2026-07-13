package com.example.titanforge.client;

import net.minecraft.entity.Entity;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Клиентский список заражённых сущностей (для подмены текстур). */
public final class ZombifiedClientCache {
    private static final Set<Integer> IDS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ZombifiedClientCache() {}

    public static void add(int entityId) {
        IDS.add(entityId);
    }

    public static boolean isZombified(Entity e) {
        return IDS.contains(e.getEntityId());
    }

    @SubscribeEvent
    public static void onWorldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote()) IDS.clear();
    }
}
