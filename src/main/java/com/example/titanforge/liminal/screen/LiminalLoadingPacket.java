package com.example.titanforge.liminal.screen;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class LiminalLoadingPacket {
    public static final int START = 0;
    public static final int PROGRESS = 1;
    public static final int STOP = 2;

    private final int action;
    private final float progress;

    public LiminalLoadingPacket(int action, float progress) {
        this.action = action;
        this.progress = progress;
    }

    public static void encode(LiminalLoadingPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.action);
        buf.writeFloat(msg.progress);
    }

    public static LiminalLoadingPacket decode(PacketBuffer buf) {
        return new LiminalLoadingPacket(buf.readVarInt(), buf.readFloat());
    }

    public static void handle(LiminalLoadingPacket msg, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        if (!context.getDirection().getReceptionSide().isClient()) return;
        context.enqueueWork(() -> {
            if (msg.action == START) {
                LiminalLoadingClient.start();
            } else if (msg.action == PROGRESS) {
                LiminalLoadingClient.setProgress(msg.progress);
            } else {
                LiminalLoadingClient.stop();
            }
        });
        context.setPacketHandled(true);
    }
}
