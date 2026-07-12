package com.example.titanforge.liminal.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.util.SoundEvents;

import java.util.Random;

public final class LiminalLoadingClient {
    private static final String[] PHRASES = {
        "Ты готов?",
        "У тебя нет права на выход.",
        "Ты не уйдёшь.",
        "Ты останешься с нами.",
        "Копия уже помнит тебя.",
        "Не оборачивайся, пока мир не собран.",
        "Последний процент принадлежит мне."
    };

    private static final Random RANDOM = new Random();

    private static boolean active;
    private static float progress;
    private static int clientTicks;
    private static int phraseIndex;
    private static int nextPhraseTick;
    private static int soundTick;
    private static int fadeOutTicks;

    private LiminalLoadingClient() {}

    public static void handlePacket(int action, float value) {
        if (action == LiminalLoadingPacket.START) start();
        else if (action == LiminalLoadingPacket.PROGRESS) setProgress(value);
        else stop();
    }

    public static void start() {
        active = true;
        progress = 0.0F;
        clientTicks = 0;
        phraseIndex = RANDOM.nextInt(PHRASES.length);
        nextPhraseTick = 30;
        soundTick = 12;
        fadeOutTicks = 0;
    }

    public static void setProgress(float value) {
        progress = Math.max(0.0F, Math.min(1.0F, value));
    }

    public static void stop() {
        active = false;
        fadeOutTicks = 30;
        progress = 1.0F;
    }

    public static void reset() {
        active = false;
        progress = 0.0F;
        fadeOutTicks = 0;
        clientTicks = 0;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        if (!active) {
            if (fadeOutTicks > 0) fadeOutTicks--;
            return;
        }

        clientTicks++;

        if (clientTicks >= nextPhraseTick) {
            phraseIndex = (phraseIndex + 1 + RANDOM.nextInt(PHRASES.length - 1))
                % PHRASES.length;
            nextPhraseTick = clientTicks + 55 + RANDOM.nextInt(50);
        }

        if (clientTicks >= soundTick) {
            playDistortedSound(mc);
            int minDelay = progress >= 0.8F ? 14 : 28;
            soundTick = clientTicks + minDelay + RANDOM.nextInt(32);
        }
    }

    private static void playDistortedSound(Minecraft mc) {
        if (mc.player == null) return;

        int pick = RANDOM.nextInt(5);
        float pitch = 0.28F + RANDOM.nextFloat() * 0.35F;
        float volume = 0.18F + progress * 0.25F;

        switch (pick) {
            case 0:
                mc.player.playSound(SoundEvents.AMBIENT_CAVE, volume, pitch);
                break;
            case 1:
                mc.player.playSound(SoundEvents.BLOCK_WOODEN_DOOR_OPEN,
                    volume * 0.8F, pitch);
                break;
            case 2:
                mc.player.playSound(SoundEvents.ENTITY_ENDERMAN_STARE,
                    volume * 0.45F, pitch);
                break;
            case 3:
                mc.player.playSound(SoundEvents.BLOCK_STONE_HIT,
                    volume * 0.65F, pitch);
                break;
            default:
                mc.player.playSound(SoundEvents.ENTITY_PLAYER_HURT,
                    volume * 0.7F, 0.45F + RANDOM.nextFloat() * 0.18F);
                break;
        }
    }

    public static boolean isActive() {
        return active;
    }

    public static float getProgress() {
        return progress;
    }

    public static String getPhrase() {
        return PHRASES[phraseIndex];
    }

    public static float getOverlayStrength(float partialTicks) {
        if (active) {
            float breath = (float) ((Math.sin((clientTicks + partialTicks) * 0.105D)
                + 1.0D) * 0.5D);
            return 0.72F + breath * 0.18F;
        }
        if (fadeOutTicks > 0) return fadeOutTicks / 30.0F * 0.72F;
        return 0.0F;
    }
}
