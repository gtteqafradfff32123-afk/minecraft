package com.example.titanforge.client;

import com.example.titanforge.ClientConfig;
import com.example.titanforge.TitanForge;
import com.example.titanforge.liminal.LiminalDimension;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class LiminalFilmController {
    private static final ResourceLocation SHADER =
            new ResourceLocation(TitanForge.MOD_ID,
                    "shaders/post/liminal_old_film.json");
    private static boolean ours;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        refreshNow();
    }

    public static void refreshNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.world == null) {
            disable(mc);
            return;
        }

        boolean insideLiminal =
                mc.world.getDimensionKey() == LiminalDimension.LIMINAL_WORLD;
        boolean shouldRun =
                ClientConfig.LIMINAL_OLD_FILM.get() && insideLiminal;

        if (shouldRun && !ours) {
            try {
                mc.gameRenderer.loadShader(SHADER);
                ours = true;
            } catch (Throwable error) {
                ours = false;
                TitanForge.LOGGER.error(
                        "[liminal-film] shader load failed", error);
            }
        } else if (!shouldRun) {
            disable(mc);
        }
    }

    private static void disable(Minecraft mc) {
        if (!ours) return;
        try {
            mc.gameRenderer.stopUseShader();
        } finally {
            ours = false;
        }
    }

    private LiminalFilmController() {}
}
