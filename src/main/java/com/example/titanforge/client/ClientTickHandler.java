package com.example.titanforge.client;

import com.example.titanforge.TitanForge;
import com.example.titanforge.NetworkHandler;
import com.example.titanforge.OpenGuiPacket;
import com.example.titanforge.liminal.LiminalDimension;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
@OnlyIn(Dist.CLIENT)
public class ClientTickHandler {

    private static boolean hadWorld;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();

        if (mc.player == null || mc.world == null) {
            LiminalMusicController.stop();
            OwnerSkinResolver.clear();
            hadWorld = false;
            return;
        }

        boolean hasWorld = mc.world != null;

        if (hadWorld && !hasWorld) {
            OwnerSkinResolver.clear();
        }
        hadWorld = hasWorld;

        boolean inLiminal = mc.world.getDimensionKey()
            .getLocation()
            .equals(LiminalDimension.LIMINAL_WORLD.getLocation());

        if (!inLiminal) {
            LiminalMusicController.stop();
        }

        if (ModKeybinds.ENCHANTER_KEY == null) return;
        while (ModKeybinds.ENCHANTER_KEY.isPressed()) {
            try {
                NetworkHandler.INSTANCE.sendToServer(new OpenGuiPacket());
            } catch (Exception e) {
                TitanForge.LOGGER.error("[TitanForge] Keybind sendToServer failed", e);
            }
        }
    }
}
