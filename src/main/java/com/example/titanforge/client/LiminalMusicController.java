package com.example.titanforge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class LiminalMusicController {
    private static ISound active;

    public static void play(String soundName) {
        stop();
        ResourceLocation id = new ResourceLocation("titanforge", soundName);
        float volume = soundName.equals("liminal_calm") ? 0.4F : 1.0F;
        active = new SimpleSound(id, SoundCategory.AMBIENT,
                volume, 1.0F, true, 0,
                ISound.AttenuationType.NONE,
                0.0D, 0.0D, 0.0D, true);
        Minecraft.getInstance().getSoundHandler().play(active);
    }

    public static boolean isPlaying() {
        return active != null;
    }

    public static void stop() {
        if (active != null) {
            Minecraft.getInstance().getSoundHandler().stop(active);
            active = null;
        }
    }

    private LiminalMusicController() {}
}
