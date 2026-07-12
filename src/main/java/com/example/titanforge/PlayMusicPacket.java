package com.example.titanforge;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayMusicPacket {
    private final String soundName;

    public PlayMusicPacket(String soundName) {
        this.soundName = soundName;
    }

    public static void encode(PlayMusicPacket msg, PacketBuffer buf) {
        buf.writeString(msg.soundName);
    }

    public static PlayMusicPacket decode(PacketBuffer buf) {
        return new PlayMusicPacket(buf.readString());
    }

    public static void handle(PlayMusicPacket msg,
                              java.util.function.Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
                com.example.titanforge.client.LiminalMusicController.play(msg.soundName));
        ctx.get().setPacketHandled(true);
    }
}
