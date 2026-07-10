package com.example.titanforge;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TitanForge.MOD_ID, "main"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        INSTANCE.registerMessage(id++, OpenGuiPacket.class, OpenGuiPacket::encode, OpenGuiPacket::decode, OpenGuiPacket::handle);
        INSTANCE.registerMessage(id++, ApplyEnchantPacket.class, ApplyEnchantPacket::encode, ApplyEnchantPacket::decode, ApplyEnchantPacket::handle);
        INSTANCE.registerMessage(id++, BloodRageSyncPacket.class, BloodRageSyncPacket::encode, BloodRageSyncPacket::decode, BloodRageSyncPacket::handle);
        INSTANCE.registerMessage(id++, SwingPacket.class, SwingPacket::encode, SwingPacket::decode, SwingPacket::handle);
        INSTANCE.registerMessage(id++, BloodPactSyncPacket.class, BloodPactSyncPacket::encode, BloodPactSyncPacket::decode, BloodPactSyncPacket::handle);
        INSTANCE.registerMessage(id++, ScreenEffectPacket.class, ScreenEffectPacket::encode, ScreenEffectPacket::decode, ScreenEffectPacket::handle);
        INSTANCE.registerMessage(id++, PlayMusicPacket.class, PlayMusicPacket::encode, PlayMusicPacket::decode, PlayMusicPacket::handle);
    }
}
