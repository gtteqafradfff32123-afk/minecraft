package com.example.titanforge;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class ApplyEnchantPacket {
    private final ResourceLocation enchantId;
    private final int level;

    public ApplyEnchantPacket(ResourceLocation enchantId, int level) {
        this.enchantId = enchantId;
        this.level = level;
    }

    public static void encode(ApplyEnchantPacket msg, PacketBuffer buf) {
        buf.writeResourceLocation(msg.enchantId);
        buf.writeInt(msg.level);
    }

    public static ApplyEnchantPacket decode(PacketBuffer buf) {
        return new ApplyEnchantPacket(buf.readResourceLocation(), buf.readInt());
    }

    public static void handle(ApplyEnchantPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null || !player.isCreative()) return;

            ItemStack stack = player.openContainer.getSlot(0).getStack();
            if (stack.isEmpty()) return;

            Enchantment enchant = Registry.ENCHANTMENT.getOptional(msg.enchantId).orElse(null);
            if (enchant != null) {
                int level = Math.min(msg.level, enchant.getMaxLevel());
                ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
                book.addEnchantment(enchant, level);
                player.openContainer.getSlot(0).putStack(book);
                player.openContainer.getSlot(0).onSlotChanged();
                player.openContainer.detectAndSendChanges();
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
