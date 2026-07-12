package com.example.titanforge.liminal.screen;

import com.example.titanforge.TitanForge;
import com.example.titanforge.liminal.LiminalDimension;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(
    modid = TitanForge.MOD_ID,
    bus = Mod.EventBusSubscriber.Bus.FORGE,
    value = Dist.CLIENT
)
public final class LiminalClientOverlay {
    private static final Random GRAIN = new Random();
    private static int movieTicks;

    private LiminalClientOverlay() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        LiminalLoadingClient.tick();
        movieTicks++;
    }

    @SubscribeEvent
    public static void onOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrix = event.getMatrixStack();
        int width = mc.getMainWindow().getScaledWidth();
        int height = mc.getMainWindow().getScaledHeight();

        if (LiminalLoadingClient.isActive()
            || LiminalLoadingClient.getOverlayStrength(event.getPartialTicks()) > 0.0F) {
            renderLoading(matrix, mc, width, height, event.getPartialTicks());
            return;
        }

        ResourceLocation current = mc.world.getDimensionKey().getLocation();
        if (current.equals(LiminalDimension.LIMINAL_WORLD.getLocation())) {
            renderOldMovie(matrix, width, height);
        }
    }

    private static void renderLoading(MatrixStack matrix, Minecraft mc,
                                      int width, int height, float partialTicks) {
        float strength = LiminalLoadingClient.getOverlayStrength(partialTicks);
        renderVignette(matrix, width, height, strength);

        if (!LiminalLoadingClient.isActive()) return;

        float progress = LiminalLoadingClient.getProgress();
        String phrase = LiminalLoadingClient.getPhrase();
        String percent = Math.round(progress * 100.0F) + "%";

        int phraseWidth = mc.fontRenderer.getStringWidth(phrase);
        int percentWidth = mc.fontRenderer.getStringWidth(percent);

        mc.fontRenderer.drawStringWithShadow(matrix, phrase,
            width / 2.0F - phraseWidth / 2.0F,
            height / 2.0F - 18.0F, 0xFFD8D8D8);

        mc.fontRenderer.drawStringWithShadow(matrix, percent,
            width / 2.0F - percentWidth / 2.0F,
            height / 2.0F + 9.0F, 0xFF686868);
    }

    private static void renderVignette(MatrixStack matrix, int width,
                                       int height, float strength) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int bands = 24;
        int maxInset = Math.max(48, Math.min(width, height) / 3);

        for (int i = 0; i < bands; i++) {
            float t = i / (float) bands;
            int alpha = (int) (strength * 24.0F * (1.0F - t));
            int color = (alpha << 24);
            int inset = i * maxInset / bands;

            AbstractGui.fill(matrix, inset, inset,
                width - inset, inset + 5, color);
            AbstractGui.fill(matrix, inset, height - inset - 5,
                width - inset, height - inset, color);
            AbstractGui.fill(matrix, inset, inset,
                inset + 5, height - inset, color);
            AbstractGui.fill(matrix, width - inset - 5, inset,
                width - inset, height - inset, color);
        }

        RenderSystem.disableBlend();
    }

    private static void renderOldMovie(MatrixStack matrix, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        AbstractGui.fill(matrix, 0, 0, width, height, 0x171C1208);

        int offset = movieTicks & 3;
        for (int y = offset; y < height; y += 4) {
            AbstractGui.fill(matrix, 0, y, width, y + 1, 0x16000000);
        }

        GRAIN.setSeed(movieTicks * 341873128712L);
        int grainCount = Math.max(90, width * height / 5200);
        for (int i = 0; i < grainCount; i++) {
            int x = GRAIN.nextInt(Math.max(1, width));
            int y = GRAIN.nextInt(Math.max(1, height));
            int size = 1 + GRAIN.nextInt(2);
            int color = GRAIN.nextBoolean() ? 0x18FFFFFF : 0x18000000;
            AbstractGui.fill(matrix, x, y, x + size, y + size, color);
        }

        if (movieTicks % 7 == 0) {
            int scratches = 1 + GRAIN.nextInt(3);
            for (int i = 0; i < scratches; i++) {
                int x = GRAIN.nextInt(Math.max(1, width));
                AbstractGui.fill(matrix, x, 0, x + 1, height, 0x22E8DEC8);
            }
        }

        int flicker = 5 + (int) ((Math.sin(movieTicks * 0.31D) + 1.0D) * 4.0D);
        AbstractGui.fill(matrix, 0, 0, width, height,
            (flicker << 24) | 0x00F0D8A0);

        renderVignette(matrix, width, height, 0.42F);

        RenderSystem.color4f(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }
}
