package com.example.titanforge.liminal.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;

import java.util.Random;

public final class LiminalLoadingClient {
    private static boolean active = false;
    private static float progress = 0.0F;
    private static int tickCount = 0;
    private static String currentPhrase = "";
    private static final Random rand = new Random();

    private static final String[][] PHRASES = {
        {"finding stable anchor...", "calibrating gaze..."},
        {"synchronizing timelines...", "unfolding paradox..."},
        {"the corridor bends...", "shadows remember..."},
        {"almost there...", "don't blink..."},
    };

    public static void start() {
        active = true;
        progress = 0.0F;
        tickCount = 0;
        currentPhrase = randomPhrase();
        sendPhrase();
    }

    public static void setProgress(float p) {
        int oldPhase = getPhase();
        progress = Math.min(1.0F, p);
        tickCount = (int)(progress * 200);
        int newPhase = getPhase();
        if (newPhase != oldPhase) {
            currentPhrase = randomPhrase();
            sendPhrase();
        }
    }

    public static void tick() {
        if (!active) return;
        tickCount++;
        progress = Math.min(1.0F, (float) tickCount / 200.0F);
        int oldPhase = getPhase();
        int newPhase = (int)(progress * 4);
        if (newPhase > oldPhase && newPhase < 4) {
            currentPhrase = randomPhrase();
            sendPhrase();
        }
    }

    public static void stop() {
        active = false;
        progress = 1.0F;
    }

    public static boolean isActive() {
        return active;
    }

    public static float getProgress() {
        return progress;
    }

    private static void sendPhrase() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(
                new StringTextComponent("\u00A78[\u00A77Liminal\u00A78] \u00A77" + currentPhrase),
                mc.player.getUniqueID());
        }
    }

    private static int getPhase() {
        if (progress < 0.25F) return 0;
        if (progress < 0.5F) return 1;
        if (progress < 0.75F) return 2;
        return 3;
    }

    private static String randomPhrase() {
        int phase = getPhase();
        String[] opts = PHRASES[Math.min(phase, PHRASES.length - 1)];
        return opts[rand.nextInt(opts.length)];
    }
}
