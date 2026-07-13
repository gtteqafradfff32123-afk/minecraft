package com.example.titanforge.client;

import com.example.titanforge.ModContainers;
import com.example.titanforge.ModEntities;
import com.example.titanforge.TitanForge;
import com.example.titanforge.EnchanterScreen;
import com.example.titanforge.client.renderer.GraveglassColossusRenderer;
import com.example.titanforge.client.renderer.PlagueDoctorRenderer;
import com.example.titanforge.liminal.screen.LiminalClientOverlay;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.entity.SpriteRenderer;
import net.minecraft.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        net.minecraftforge.fml.ModLoadingContext.get().registerExtensionPoint(
                net.minecraftforge.fml.ExtensionPoint.CONFIGGUIFACTORY,
                () -> (minecraft, parent) ->
                        new com.example.titanforge.client.TitanForgeConfigScreen(parent));

        event.enqueueWork(() -> {
            ScreenManager.registerFactory(ModContainers.ENCHANTER.get(), EnchanterScreen::new);
            ModKeybinds.register();
        });

        MinecraftForge.EVENT_BUS.register(LiminalFilmController.class);
        MinecraftForge.EVENT_BUS.register(LiminalClientOverlay.class);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.PLAGUE_DOCTOR.get(), PlagueDoctorRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.SHADOW.get(), ShadowRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.PLAYER_COPY.get(), PlayerCopyRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.STUN_ZOMBIE.get(), StunZombieRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityType.ZOMBIE, LiminalZombieRenderer::new);
        RenderingRegistry.registerEntityRenderingHandler(ModEntities.GRAVEGLASS_COLOSSUS.get(), GraveglassColossusRenderer::new);
        com.example.titanforge.client.renderer.ZombifiedRenderers.register();
        MinecraftForge.EVENT_BUS.register(ZombifiedClientCache.class);
    }
}
