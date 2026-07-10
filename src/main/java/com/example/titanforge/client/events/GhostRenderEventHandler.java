package com.example.titanforge.client.events;

import com.example.titanforge.TitanForge;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class GhostRenderEventHandler {

    private static final ThreadLocal<Boolean> RENDERING = ThreadLocal.withInitial(() -> false);

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderPre(RenderLivingEvent.Pre event) {
        LivingEntity entity = event.getEntity();
        if (RENDERING.get()) return;

        ITextComponent customName = entity.getCustomName();
        if (customName == null) return;
        String name = customName.getString();
        boolean isThrall = name.contains("Risen Thrall");
        boolean isGhost = !isThrall && name.contains("Soul Copy");
        if (!isGhost && !isThrall) return;

        event.setCanceled(true);
        RENDERING.set(true);
        try {
            LivingRenderer renderer = (LivingRenderer) event.getRenderer();
            IRenderTypeBuffer original = event.getBuffers();
            ResourceLocation texture = renderer.getEntityTexture(entity);
            RenderType cutoutType = RenderType.getEntityCutoutNoCull(texture);
            RenderType translucentType = RenderType.getEntityTranslucent(texture);

            IRenderTypeBuffer wrapped = type -> {
                IVertexBuilder buf = original.getBuffer(type == cutoutType ? translucentType : type);
                if (isThrall) {
                    return new TintedAlphaBuilder(buf, 0.75F, 0.55F, 1.0F, 0.35F);
                }
                return new AlphaVertexBuilder(buf, 0.35F);
            };

            renderer.render(entity, entity.rotationYaw, event.getPartialRenderTick(),
                    event.getMatrixStack(), wrapped, event.getLight());
        } finally {
            RENDERING.set(false);
        }
    }

    private static class AlphaVertexBuilder implements IVertexBuilder {
        private final IVertexBuilder d;
        private final float multiplier;

        AlphaVertexBuilder(IVertexBuilder delegate, float multiplier) {
            this.d = delegate;
            this.multiplier = multiplier;
        }

        @Override public IVertexBuilder pos(double x, double y, double z) { return d.pos(x, y, z); }
        @Override public IVertexBuilder color(int red, int green, int blue, int alpha) {
            return d.color((int) (red * 0.7F), (int) (green * 0.8F), blue, (int) (alpha * multiplier));
        }
        @Override public IVertexBuilder color(float red, float green, float blue, float alpha) {
            return d.color(red * 0.7F, green * 0.8F, blue, alpha * multiplier);
        }
        @Override public IVertexBuilder tex(float u, float v) { return d.tex(u, v); }
        @Override public IVertexBuilder overlay(int u, int v) { return d.overlay(u, v); }
        @Override public IVertexBuilder lightmap(int u, int v) { return d.lightmap(u, v); }
        @Override public IVertexBuilder normal(float x, float y, float z) { return d.normal(x, y, z); }
        @Override public void endVertex() { d.endVertex(); }
    }

    private static class TintedAlphaBuilder implements IVertexBuilder {
        private final IVertexBuilder d;
        private final float r, g, b, a;

        TintedAlphaBuilder(IVertexBuilder delegate, float r, float g, float b, float a) {
            this.d = delegate; this.r = r; this.g = g; this.b = b; this.a = a;
        }

        @Override public IVertexBuilder pos(double x, double y, double z) { return d.pos(x, y, z); }
        @Override public IVertexBuilder color(int red, int green, int blue, int alpha) {
            return d.color((int)(red * r), (int)(green * g), (int)(blue * b), (int)(alpha * a));
        }
        @Override public IVertexBuilder color(float red, float green, float blue, float alpha) {
            return d.color(red * r, green * g, blue * b, alpha * a);
        }
        @Override public IVertexBuilder tex(float u, float v) { return d.tex(u, v); }
        @Override public IVertexBuilder overlay(int u, int v) { return d.overlay(u, v); }
        @Override public IVertexBuilder lightmap(int u, int v) { return d.lightmap(u, v); }
        @Override public IVertexBuilder normal(float x, float y, float z) { return d.normal(x, y, z); }
        @Override public void endVertex() { d.endVertex(); }
    }
}
