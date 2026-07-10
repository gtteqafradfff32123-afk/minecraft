package com.example.titanforge.client;

import com.example.titanforge.NetworkHandler;
import com.example.titanforge.OpenGuiPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
@OnlyIn(Dist.CLIENT)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) return;
        if (ModKeybinds.ENCHANTER_KEY == null) return;
        while (ModKeybinds.ENCHANTER_KEY.isPressed()) {
            try {
                NetworkHandler.INSTANCE.sendToServer(new OpenGuiPacket());
            } catch (Exception e) {
                System.out.println("[TitanForge] Keybind sendToServer failed: " + e);
            }
        }
    }
}
