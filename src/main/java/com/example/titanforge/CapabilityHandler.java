package com.example.titanforge;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class CapabilityHandler {
    public static final ResourceLocation MEMBRANE_ID = new ResourceLocation(TitanForge.MOD_ID, "membrane");

    @SubscribeEvent
    public void onAttachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof PlayerEntity) {
            event.addCapability(MEMBRANE_ID, new MembraneCapability.Provider());
        }
    }
}
