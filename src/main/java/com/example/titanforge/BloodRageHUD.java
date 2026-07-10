package com.example.titanforge;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class BloodRageHUD {

    private static final int MAX_CHARGE = 200;
    private static final int BAR_WIDTH = 100;
    private static final int BAR_HEIGHT = 5;

    @SubscribeEvent
    public static void onRenderHUD(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getInstance();
        ClientPlayerEntity player = mc.player;
        if (player == null) return;

        int charge = player.getPersistentData().getInt("BloodRageCharge");
        if (charge <= 0) return;

        ItemStack weapon = player.getHeldItemMainhand();
        boolean hasValidEnchant =
            EnchantmentHelper.getEnchantmentLevel(ModEnchantments.CHAOS_DEVOUR.get(), weapon) > 0;
        if (!hasValidEnchant) return;

        int width = mc.getMainWindow().getScaledWidth();
        int height = mc.getMainWindow().getScaledHeight();

        int x = width / 2 - BAR_WIDTH / 2;
        int y = height - 55;

        MatrixStack stack = event.getMatrixStack();

        // Background
        fill(stack, x - 1, y - 1, x + BAR_WIDTH + 1, y + BAR_HEIGHT + 2, 0x90000000);

        // Bar color
        int barColor;
        if (charge >= MAX_CHARGE) {
            barColor = player.ticksExisted % 10 < 5 ? 0xFFFF5500 : 0xFFFF0000;
        } else {
            float t = (float) charge / MAX_CHARGE;
            int r = (int) (180 + (75 * t));
            barColor = (0xFF << 24) | (r << 16) | (0 << 8) | (0);
        }

        // Fill
        int fillW = charge * BAR_WIDTH / MAX_CHARGE;
        if (fillW > 0) {
            fill(stack, x, y, x + fillW, y + BAR_HEIGHT, barColor);
        }

        // Label
        String label = charge >= MAX_CHARGE
            ? "\u00A7c\u00A7lBLOOD RAGE READY!"
            : "\u00A7c" + (charge * 100 / MAX_CHARGE) + "%";
        int tw = mc.fontRenderer.getStringWidth(label);
        mc.fontRenderer.drawStringWithShadow(stack, label, x + BAR_WIDTH / 2.0F - tw / 2.0F, y - 10, 0xFFFFFFFF);
    }

    private static void fill(MatrixStack matrixStack, int minX, int minY, int maxX, int maxY, int color) {
        net.minecraft.client.gui.AbstractGui.fill(matrixStack, minX, minY, maxX, maxY, color);
    }
}
