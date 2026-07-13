package com.example.titanforge.client.renderer;

import com.example.titanforge.TitanForge;
import com.example.titanforge.client.ZombifiedClientCache;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.ChickenRenderer;
import net.minecraft.client.renderer.entity.CowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.FoxRenderer;
import net.minecraft.client.renderer.entity.IEntityRenderer;
import net.minecraft.client.renderer.entity.PigRenderer;
import net.minecraft.client.renderer.entity.RabbitRenderer;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.client.renderer.entity.SpiderRenderer;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.layers.SheepWoolLayer;
import net.minecraft.client.renderer.entity.model.SheepModel;
import net.minecraft.client.renderer.entity.model.SheepWoolModel;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.passive.ChickenEntity;
import net.minecraft.entity.passive.CowEntity;
import net.minecraft.entity.passive.FoxEntity;
import net.minecraft.entity.passive.PigEntity;
import net.minecraft.entity.passive.RabbitEntity;
import net.minecraft.entity.passive.SheepEntity;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.RenderingRegistry;

/** Подмена рендеров ванильных мобов: заражённые получают гнилую текстуру. */
public final class ZombifiedRenderers {
    private static ResourceLocation tex(String name) {
        return new ResourceLocation(TitanForge.MOD_ID, "textures/entity/zombified/" + name + ".png");
    }

    private static final ResourceLocation COW = tex("cow");
    private static final ResourceLocation PIG = tex("pig");
    private static final ResourceLocation SHEEP = tex("sheep");
    static final ResourceLocation SHEEP_FUR = tex("sheep_fur");
    private static final ResourceLocation CHICKEN = tex("chicken");
    private static final ResourceLocation WOLF = tex("wolf");
    private static final ResourceLocation FOX = tex("fox");
    private static final ResourceLocation RABBIT = tex("rabbit");
    private static final ResourceLocation SPIDER = tex("spider");

    private ZombifiedRenderers() {}

    public static void register() {
        RenderingRegistry.registerEntityRenderingHandler(EntityType.COW, mgr -> new CowRenderer(mgr) {
            @Override public ResourceLocation getEntityTexture(CowEntity e) {
                return ZombifiedClientCache.isZombified(e) ? COW : super.getEntityTexture(e);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityType.PIG, mgr -> new PigRenderer(mgr) {
            @Override public ResourceLocation getEntityTexture(PigEntity e) {
                return ZombifiedClientCache.isZombified(e) ? PIG : super.getEntityTexture(e);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityType.CHICKEN, mgr -> new ChickenRenderer(mgr) {
            @Override public ResourceLocation getEntityTexture(ChickenEntity e) {
                return ZombifiedClientCache.isZombified(e) ? CHICKEN : super.getEntityTexture(e);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityType.WOLF, mgr -> new WolfRenderer(mgr) {
            @Override public ResourceLocation getEntityTexture(WolfEntity e) {
                return ZombifiedClientCache.isZombified(e) ? WOLF : super.getEntityTexture(e);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityType.FOX, mgr -> new FoxRenderer(mgr) {
            @Override public ResourceLocation getEntityTexture(FoxEntity e) {
                return ZombifiedClientCache.isZombified(e) ? FOX : super.getEntityTexture(e);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityType.RABBIT, mgr -> new RabbitRenderer(mgr) {
            @Override public ResourceLocation getEntityTexture(RabbitEntity e) {
                return ZombifiedClientCache.isZombified(e) ? RABBIT : super.getEntityTexture(e);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityType.SPIDER, mgr -> new SpiderRenderer<SpiderEntity>(mgr) {
            @Override public ResourceLocation getEntityTexture(SpiderEntity e) {
                return ZombifiedClientCache.isZombified(e) ? SPIDER : super.getEntityTexture(e);
            }
        });
        RenderingRegistry.registerEntityRenderingHandler(EntityType.SHEEP, ZombifiedSheepRenderer::new);
    }

    /** Овца: тело + шерсть меняют текстуру, шерсть дополнительно гниловато тонируется. */
    public static class ZombifiedSheepRenderer extends SheepRenderer {
        public ZombifiedSheepRenderer(EntityRendererManager mgr) {
            super(mgr);
            this.layerRenderers.removeIf(l -> l instanceof SheepWoolLayer);
            this.addLayer(new ZombifiedWoolLayer(this));
        }

        @Override
        public ResourceLocation getEntityTexture(SheepEntity e) {
            return ZombifiedClientCache.isZombified(e) ? SHEEP : super.getEntityTexture(e);
        }
    }

    /** Копия ванильного SheepWoolLayer с динамической текстурой (без радуги jeb_). */
    static class ZombifiedWoolLayer extends LayerRenderer<SheepEntity, SheepModel<SheepEntity>> {
        private static final ResourceLocation VANILLA_FUR = new ResourceLocation("textures/entity/sheep/sheep_fur.png");
        private final SheepWoolModel<SheepEntity> sheepModel = new SheepWoolModel<>();

        ZombifiedWoolLayer(IEntityRenderer<SheepEntity, SheepModel<SheepEntity>> renderer) {
            super(renderer);
        }

        @Override
        public void render(MatrixStack matrixStack, IRenderTypeBuffer buffer, int packedLight, SheepEntity sheep,
                           float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                           float netHeadYaw, float headPitch) {
            if (sheep.getSheared() || sheep.isInvisible()) return;
            boolean zombified = ZombifiedClientCache.isZombified(sheep);
            float[] rgb = SheepEntity.getDyeRgb(sheep.getFleeceColor());
            float r = rgb[0], g = rgb[1], b = rgb[2];
            if (zombified) {
                r *= 0.55F; g *= 0.85F; b *= 0.5F;
            }
            renderCopyCutoutModel(this.getEntityModel(), this.sheepModel,
                    zombified ? SHEEP_FUR : VANILLA_FUR,
                    matrixStack, buffer, packedLight, sheep,
                    limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, partialTicks, r, g, b);
        }
    }
}
