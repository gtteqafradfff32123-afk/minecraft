package com.example.titanforge;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class StopMusicPacket {
    public static void encode(StopMusicPacket msg, PacketBuffer buf) {}
    public static StopMusicPacket decode(PacketBuffer buf) { return new StopMusicPacket(); }
    public static void handle(StopMusicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> Minecraft.getInstance().getSoundHandler().stop());
        ctx.get().setPacketHandled(true);
    }
}
