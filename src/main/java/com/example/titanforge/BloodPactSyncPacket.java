package com.example.titanforge;

import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class BloodPactSyncPacket {
    private final boolean active;

    public BloodPactSyncPacket(boolean active) {
        this.active = active;
    }

    public static void encode(BloodPactSyncPacket msg, PacketBuffer buf) {
        buf.writeBoolean(msg.active);
    }

    public static BloodPactSyncPacket decode(PacketBuffer buf) {
        return new BloodPactSyncPacket(buf.readBoolean());
    }

    public static void handle(BloodPactSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.getPersistentData().putBoolean("BloodPactActive", msg.active);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
