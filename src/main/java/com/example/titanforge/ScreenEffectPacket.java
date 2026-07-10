package com.example.titanforge;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ScreenEffectPacket {
    private final int effectId;
    private final int duration;

    public ScreenEffectPacket(int effectId, int duration) {
        this.effectId = effectId;
        this.duration = duration;
    }

    public static void encode(ScreenEffectPacket msg, PacketBuffer buf) {
        buf.writeInt(msg.effectId);
        buf.writeInt(msg.duration);
    }

    public static ScreenEffectPacket decode(PacketBuffer buf) {
        return new ScreenEffectPacket(buf.readInt(), buf.readInt());
    }

    public static void handle(ScreenEffectPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.getPersistentData().putInt("ScreenEffect" + msg.effectId, msg.duration);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
