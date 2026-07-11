package com.example.titanforge.client;

import com.example.titanforge.ModContainers;
import com.example.titanforge.ModEntities;
import com.example.titanforge.TitanForge;
import com.example.titanforge.EnchanterScreen;
import com.example.titanforge.client.renderer.PlagueDoctorRenderer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.entity.SpriteRenderer;
import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() ->
            ScreenManager.registerFactory(ModContainers.ENCHANTER.get(), EnchanterScreen::new)
        );
        ModKeybinds.register();
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.PLAGUE_DOCTOR.get(), PlagueDoctorRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.SHADOW.get(), ShadowRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.PLAYER_COPY.get(), PlayerCopyRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.STUN_ZOMBIE.get(), StunZombieRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityType.ZOMBIE, LiminalZombieRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.BLIND_GOLEM.get(),
                m -> new net.minecraft.client.renderer.entity.MobRenderer<com.example.titanforge.entities.BlindGolemEntity, net.minecraft.client.renderer.entity.model.BipedModel<com.example.titanforge.entities.BlindGolemEntity>>(
                        m, new net.minecraft.client.renderer.entity.model.BipedModel<>(0.0F), 0.7F) {
                    @Override
                    public net.minecraft.util.ResourceLocation getEntityTexture(com.example.titanforge.entities.BlindGolemEntity e) {
                        return new net.minecraft.util.ResourceLocation("titanforge", "textures/entity/blind_golem.png");
                    }
                });
    }
}
