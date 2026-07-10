package com.example.titanforge.liminal.screen;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.server.STitlePacket;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.text.StringTextComponent;

public class LimboHandler {

    public static void enterLimbo(ServerPlayerEntity player) {
        player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, 20 * 30, 0, false, false));
        player.addPotionEffect(new EffectInstance(Effects.SLOWNESS, 20 * 30, 250, false, false));
        player.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE,
            new StringTextComponent("\u00A70. . .")));
    }

    public static void updateProgress(ServerPlayerEntity player, float pct) {
        int p = Math.round(pct * 100);
        player.connection.sendPacket(new STitlePacket(STitlePacket.Type.SUBTITLE,
            new StringTextComponent("\u00A78immersion " + p + "% \u00A77" + getBar(p))));
    }

    private static String getBar(int pct) {
        int filled = pct / 10;
        StringBuilder sb = new StringBuilder("\u00A78[");
        for (int i = 0; i < 10; i++) {
            sb.append(i < filled ? "\u00A77\u2588" : "\u00A78\u2588");
        }
        sb.append("\u00A78]");
        return sb.toString();
    }

    public static void exitLimbo(ServerPlayerEntity player) {
        player.removePotionEffect(Effects.BLINDNESS);
        player.removePotionEffect(Effects.SLOWNESS);
        player.connection.sendPacket(new STitlePacket(STitlePacket.Type.TITLE,
            new StringTextComponent("")));
    }
}
