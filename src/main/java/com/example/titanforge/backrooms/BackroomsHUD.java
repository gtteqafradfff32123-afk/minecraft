package com.example.titanforge.backrooms;

import com.example.titanforge.TitanForge;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID, value = Dist.CLIENT)
public final class BackroomsHUD {
    public static volatile int pressure;
    public static volatile int errorsFound;
    public static volatile boolean building;
    public static volatile boolean finished;

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!finished && pressure <= 0) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        MatrixStack stack = event.getMatrixStack();
        int w = event.getWindow().getScaledWidth();
        int h = event.getWindow().getScaledHeight();

        String pressureText = "§fДавление: §e" + pressure + "%";
        mc.fontRenderer.drawStringWithShadow(stack, pressureText, 10, h - 30, 0xFFFFFF);

        if (errorsFound > 0) {
            String errorText = "§fОшибки: §c" + errorsFound + "/3";
            mc.fontRenderer.drawStringWithShadow(stack, errorText, 10, h - 20, 0xFFFFFF);
        }

        if (building) {
            String buildText = "§7Сборка...";
            mc.fontRenderer.drawStringWithShadow(stack, buildText, 10, h - 10, 0xFFFFFF);
        }

        if (finished) {
            String finishText = "§a§lЗавершено";
            int tw = mc.fontRenderer.getStringWidth(finishText);
            mc.fontRenderer.drawStringWithShadow(stack, finishText, (w - tw) / 2.0F, h / 2.0F - 10, 0xFFFFFF);
        }
    }
}
