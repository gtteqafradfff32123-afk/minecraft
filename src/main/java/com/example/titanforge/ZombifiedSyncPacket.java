package com.example.titanforge;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/** Сообщает клиенту, что сущность заражена — рендер подменит текстуру на гнилую. */
public class ZombifiedSyncPacket {
    private final int entityId;

    public ZombifiedSyncPacket(int entityId) {
        this.entityId = entityId;
    }

    public static void encode(ZombifiedSyncPacket msg, PacketBuffer buf) {
        buf.writeVarInt(msg.entityId);
    }

    public static ZombifiedSyncPacket decode(PacketBuffer buf) {
        return new ZombifiedSyncPacket(buf.readVarInt());
    }

    public static void handle(ZombifiedSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                com.example.titanforge.client.ZombifiedClientCache.add(msg.entityId));
        ctx.get().setPacketHandled(true);
    }
}
