package com.example.titanforge;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class BloodRageSyncPacket {
    private final int charge;

    public BloodRageSyncPacket(int charge) {
        this.charge = charge;
    }

    public static void encode(BloodRageSyncPacket msg, PacketBuffer buf) {
        buf.writeInt(msg.charge);
    }

    public static BloodRageSyncPacket decode(PacketBuffer buf) {
        return new BloodRageSyncPacket(buf.readInt());
    }

    public static void handle(BloodRageSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            PlayerEntity player = Minecraft.getInstance().player;
            if (player != null) {
                player.getPersistentData().putInt("BloodRageCharge", msg.charge);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
