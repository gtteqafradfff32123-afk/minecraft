package com.example.titanforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class PlayOneShotSoundPacket {
    private final String name;

    public PlayOneShotSoundPacket(String name) { this.name = name; }
    public static void encode(PlayOneShotSoundPacket m, PacketBuffer b) { b.writeString(m.name); }
    public static PlayOneShotSoundPacket decode(PacketBuffer b) {
        return new PlayOneShotSoundPacket(b.readString(128));
    }
    public static void handle(PlayOneShotSoundPacket m, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> Minecraft.getInstance().getSoundHandler().play(
                SimpleSound.master(new SoundEvent(new ResourceLocation("titanforge", m.name)), 1.0F)));
        ctx.get().setPacketHandled(true);
    }
}
