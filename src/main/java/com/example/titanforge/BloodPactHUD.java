package com.example.titanforge;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BloodPactHUD {

    @SubscribeEvent
    public static void onRenderHUD(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        if (player.getPersistentData().getBoolean("BloodPactActive")) {
            int width = mc.getMainWindow().getScaledWidth();

            String label = "BLOOD PACT";
            int heartWidth = mc.fontRenderer.getStringWidth("\u2764 ");
            int labelWidth = mc.fontRenderer.getStringWidth(label);
            int totalWidth = heartWidth + labelWidth;
            int startX = (width - totalWidth) / 2;
            int y = mc.getMainWindow().getScaledHeight() - 62;

            MatrixStack stack = event.getMatrixStack();

            float pulse = (float) (Math.sin(player.ticksExisted * 0.4F) * 0.5 + 0.5);
            int alpha = (int) (100 + 155 * pulse);
            int heartColor = (alpha << 24) | 0xFF0000;

            mc.fontRenderer.drawStringWithShadow(stack, "\u2764 ", startX, y, heartColor);
            mc.fontRenderer.drawStringWithShadow(stack, label, startX + heartWidth, y, 0xFFFF5555);
        }
    }
}
