package com.example.titanforge.liminal.screen;

import com.example.titanforge.NetworkHandler;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraftforge.fml.network.PacketDistributor;

public final class LimboHandler {
    private static final int TOTAL_TICKS = 200;

    public static void enterLimbo(ServerPlayerEntity player) {
        player.addPotionEffect(new EffectInstance(Effects.BLINDNESS, TOTAL_TICKS + 40, 0, false, false));
        player.addPotionEffect(new EffectInstance(Effects.SLOWNESS, TOTAL_TICKS + 40, 4, false, false));
        NetworkHandler.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            new LiminalLoadingPacket(LiminalLoadingPacket.START, 0.0F));
    }

    private static int getLoadingTicks(ServerPlayerEntity player) {
        com.example.titanforge.liminal.LiminalManager.State st =
            com.example.titanforge.liminal.LiminalManager.getState(player.getUniqueID());
        return st != null ? st.ambientTimer : 0;
    }

    public static void tickProgress(ServerPlayerEntity player) {
        int ticks = getLoadingTicks(player);
        float progress = Math.min(1.0F, (float) ticks / TOTAL_TICKS);
        NetworkHandler.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            new LiminalLoadingPacket(LiminalLoadingPacket.PROGRESS, progress));
    }

    public static void updateProgress(ServerPlayerEntity player, float progress) {
        NetworkHandler.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            new LiminalLoadingPacket(LiminalLoadingPacket.PROGRESS, progress));
    }

    public static void exitLimbo(ServerPlayerEntity player) {
        NetworkHandler.INSTANCE.send(
            PacketDistributor.PLAYER.with(() -> player),
            new LiminalLoadingPacket(LiminalLoadingPacket.STOP, 1.0F));
    }

    private LimboHandler() {}
}
