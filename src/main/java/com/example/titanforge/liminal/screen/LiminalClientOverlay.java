package com.example.titanforge.liminal.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class LiminalClientOverlay {
    private LiminalClientOverlay() {}

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        LiminalLoadingClient.tick();
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

}

