package com.example.titanforge.liminal.screen;

import com.example.titanforge.TitanForge;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;


import java.util.Random;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class LiminalClientOverlay {
    private static final ResourceLocation VIGNETTE = new ResourceLocation("textures/misc/vignette.png");
    private static final Random rand = new Random();
    private static boolean renderedThisFrame = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            LiminalLoadingClient.tick();
        }
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (renderedThisFrame) return;
        if (!LiminalLoadingClient.isActive()) return;
        renderedThisFrame = true;

        Minecraft mc = Minecraft.getInstance();
        int w = mc.getMainWindow().getScaledWidth();
        int h = mc.getMainWindow().getScaledHeight();
        float prog = LiminalLoadingClient.getProgress();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        drawVignette(mc, w, h);
        drawGrayscale(w, h, prog);
        drawLoadingBar(w, h, prog);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    public static void onPostOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            renderedThisFrame = false;
        }
    }

    private static void drawVignette(Minecraft mc, int w, int h) {
        mc.getTextureManager().bindTexture(VIGNETTE);
        RenderSystem.color4f(0.0F, 0.0F, 0.0F, 0.9F);
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_TEX);
        buf.pos(0, 0, 0).tex(0, 0).endVertex();
        buf.pos(w, 0, 0).tex(1, 0).endVertex();
        buf.pos(w, h, 0).tex(1, 1).endVertex();
        buf.pos(0, h, 0).tex(0, 1).endVertex();
        tess.draw();
    }

    private static void drawGrayscale(int w, int h, float progress) {
        float strength = (1.0F - progress) * 0.35F;
        if (strength <= 0.0F) return;

        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(0x8001, 0x8002, 1, 0);
        RenderSystem.blendColor(1.0F - strength, 1.0F - strength, 1.0F - strength, 1.0F);

        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION);
        buf.pos(0, 0, 0).endVertex();
        buf.pos(w, 0, 0).endVertex();
        buf.pos(w, h, 0).endVertex();
        buf.pos(0, h, 0).endVertex();
        tess.draw();
    }

    private static void drawLoadingBar(int w, int h, float progress) {
        int barW = w * 2 / 3;
        int barH = 4;
        int barX = (w - barW) / 2;
        int barY = h - 40;

        fill(barX, barY, barX + barW, barY + barH, 0xFFFFFFFF);
        fill(barX + 1, barY + 1, barX + barW - 1, barY + barH - 1, 0xFF000000);
        fill(barX + 1, barY + 1, barX + 1 + (int)((barW - 2) * progress), barY + barH - 1, 0xFFFFFFFF);
    }

    private static void fill(int left, int top, int right, int bottom, int color) {
        if (left >= right || top >= bottom) return;
        float a = (color >> 24 & 255) / 255.0F;
        float r = (color >> 16 & 255) / 255.0F;
        float g = (color >> 8 & 255) / 255.0F;
        float b = (color & 255) / 255.0F;
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.getBuffer();
        buf.begin(7, DefaultVertexFormats.POSITION_COLOR);
        buf.pos(left, bottom, 0).color(r, g, b, a).endVertex();
        buf.pos(right, bottom, 0).color(r, g, b, a).endVertex();
        buf.pos(right, top, 0).color(r, g, b, a).endVertex();
        buf.pos(left, top, 0).color(r, g, b, a).endVertex();
        tess.draw();
    }
}
