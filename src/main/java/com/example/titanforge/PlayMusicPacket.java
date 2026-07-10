package com.example.titanforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayMusicPacket {
    private final String soundName;

    public PlayMusicPacket(String soundName) {
        this.soundName = soundName;
    }

    public static void encode(PlayMusicPacket msg, PacketBuffer buf) {
        buf.writeString(msg.soundName);
    }

    public static PlayMusicPacket decode(PacketBuffer buf) {
        return new PlayMusicPacket(buf.readString());
    }

    public static void handle(PlayMusicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    mc.getSoundHandler().play(new FadingSound(msg.soundName, SoundCategory.AMBIENT));
                }
            } catch (Exception e) {
                com.example.titanforge.TitanForge.LOGGER.error("[PlayMusicPacket] failed: {}", e.getMessage());
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static class FadingSound extends SimpleSound {
        private final long startTime = System.currentTimeMillis();
        private static final long FADE_DURATION = 3000;

        public FadingSound(String soundName, SoundCategory category) {
            super(new net.minecraft.util.ResourceLocation("titanforge", soundName),
                  category, 0.2F, 1.0F, true, 0,
                  ISound.AttenuationType.NONE, 0.0D, 0.0D, 0.0D, true);
        }

        @Override
        public float getVolume() {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1.0F, (float) elapsed / FADE_DURATION);
            return Math.max(0.05F, progress * 0.5F);
        }
    }
}
