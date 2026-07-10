package com.example.titanforge;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenGuiPacket {
    public OpenGuiPacket() {}

    public static void encode(OpenGuiPacket msg, PacketBuffer buf) {}
    public static OpenGuiPacket decode(PacketBuffer buf) { return new OpenGuiPacket(); }

    public static void handle(OpenGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player != null && player.isCreative()) {
                player.openContainer(new INamedContainerProvider() {
                    @Override
                    public ITextComponent getDisplayName() {
                        return new StringTextComponent("Enchanter");
                    }

                    @Override
                    public Container createMenu(int id, PlayerInventory inv, PlayerEntity p) {
                        return ModContainers.ENCHANTER.get().create(id, inv);
                    }
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
