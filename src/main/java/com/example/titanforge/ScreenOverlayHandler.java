package com.example.titanforge;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ScreenOverlayHandler {

    private static final ResourceLocation VIGNETTE_TEXTURE = new ResourceLocation("textures/misc/vignette.png");

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        try {
            if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

            Minecraft mc = Minecraft.getInstance();
            PlayerEntity player = mc.player;

            if (player == null) return;

            int width = mc.getMainWindow().getScaledWidth();
            int height = mc.getMainWindow().getScaledHeight();
            MatrixStack stack = event.getMatrixStack();

            // 1. Red tint (Absolute Blood)
            int redTint = player.getPersistentData().getInt("ScreenEffect1");
            if (redTint > 0) {
                player.getPersistentData().putInt("ScreenEffect1", redTint - 1);
                float alpha = Math.min(1.0F, redTint / 20.0F);
                renderColoredVignette(stack, 1.0F, 0.0F, 0.0F, alpha * 0.6F, width, height);
            }

            // 2. White flash (Soul Reaper)
            int whiteFlash = player.getPersistentData().getInt("ScreenEffect2");
            if (whiteFlash > 0) {
                player.getPersistentData().putInt("ScreenEffect2", whiteFlash - 1);
                float alpha = Math.min(1.0F, whiteFlash / 10.0F);
                renderSolidColor(stack, 0xFFFFFF, alpha, width, height);
            }

            // 3. Purple filter (Phase Rupture)
            int purpleFilter = player.getPersistentData().getInt("ScreenEffect3");
            if (purpleFilter > 0) {
                player.getPersistentData().putInt("ScreenEffect3", purpleFilter - 1);
                renderSolidColor(stack, 0x8A2BE2, 0.3F, width, height);
            }

            // 4. Dark blur/pulsation (Harvest of Agony)
            int darkBlur = player.getPersistentData().getInt("ScreenEffect4");
            if (darkBlur > 0) {
                player.getPersistentData().putInt("ScreenEffect4", darkBlur - 1);
                float pulse = 0.4F + (float)(Math.sin(player.ticksExisted * 0.5F) * 0.2);
                renderColoredVignette(stack, 0.0F, 0.0F, 0.0F, pulse, width, height);
            }

            // Legacy: Blood Frenzy red vignette
            EffectInstance effect = player.getActivePotionEffect(ModEffects.BLOOD_FRENZY.get());
            if (effect != null) {
                float pulseSpeed = 0.15F;
                float pulse = (float)(Math.sin(player.ticksExisted * pulseSpeed) * 0.5 + 0.5);
                float alpha = 0.1F + (pulse * 0.15F);
                if (effect.getDuration() < 200) {
                    alpha *= (player.ticksExisted % 10 < 5 ? 1.0F : 0.3F);
                }
                renderColoredVignette(stack, 1.0F, 0.0F, 0.0F, alpha, width, height);
            }
        } catch (Throwable t) {
            TitanForge.LOGGER.error("[TitanForge-ScreenOverlay] Error in event", t);
        }
    }

    private static void renderSolidColor(MatrixStack stack, int rgb, float alpha, int width, int height) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        int a = (int)(alpha * 255);
        int color = (a << 24) | (rgb & 0xFFFFFF);
        AbstractGui.fill(stack, 0, 0, width, height, color);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderColoredVignette(MatrixStack matrixStack, float r, float g, float b, float alpha, int width, int height) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        RenderSystem.color4f(r, g, b, alpha);

        Minecraft.getInstance().getTextureManager().bindTexture(VIGNETTE_TEXTURE);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        buffer.pos(0, height, 0).tex(0, 1).endVertex();
        buffer.pos(width, height, 0).tex(1, 1).endVertex();
        buffer.pos(width, 0, 0).tex(1, 0).endVertex();
        buffer.pos(0, 0, 0).tex(0, 0).endVertex();

        tessellator.draw();

        RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
