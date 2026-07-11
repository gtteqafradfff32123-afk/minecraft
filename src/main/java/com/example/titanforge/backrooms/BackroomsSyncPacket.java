package com.example.titanforge.backrooms;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class BackroomsSyncPacket {
    private final int pressure;
    private final int errorsFound;
    private final boolean building;
    private final boolean finished;

    public BackroomsSyncPacket(int pressure, int errorsFound, boolean building, boolean finished) {
        this.pressure = pressure;
        this.errorsFound = errorsFound;
        this.building = building;
        this.finished = finished;
    }

    public static void encode(BackroomsSyncPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.pressure);
        buf.writeVarInt(msg.errorsFound);
        buf.writeBoolean(msg.building);
        buf.writeBoolean(msg.finished);
    }

    public static BackroomsSyncPacket decode(PacketBuffer buf) {
        return new BackroomsSyncPacket(
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readBoolean()
        );
    }

    public static void handle(BackroomsSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            BackroomsHUD.pressure = msg.pressure;
            BackroomsHUD.errorsFound = msg.errorsFound;
            BackroomsHUD.building = msg.building;
            BackroomsHUD.finished = msg.finished;
        });
        ctx.get().setPacketHandled(true);
    }
}
