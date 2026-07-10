package com.example.titanforge;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class SwingPacket {
    public SwingPacket() {}

    public static void encode(SwingPacket msg, PacketBuffer buf) {}
    public static SwingPacket decode(PacketBuffer buf) { return new SwingPacket(); }

    public static void handle(SwingPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayerEntity player = ctx.get().getSender();
            if (player == null) return;

            CompoundNBT data = player.getPersistentData();
            long now = player.world.getGameTime();
            if (now == data.getLong("TF_BloodDrainTick")) return;
            data.putLong("TF_BloodDrainTick", now);

            if (data.getBoolean("BloodPactActive")) {
                ItemStack weapon = player.getItemStackFromSlot(EquipmentSlotType.MAINHAND);
                int bloodLvl = net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(ModEnchantments.BLOOD_PACT.get(), weapon);

                if (bloodLvl > 0) {
                    float drain = bloodLvl;
                    if (player.getHealth() <= 2.0F + drain) {
                        data.putBoolean("BloodPactActive", false);
                        data.putLong("BloodPactCooldown", player.world.getGameTime() + 200L);
                        NetworkHandler.INSTANCE.send(
                            net.minecraftforge.fml.network.PacketDistributor.PLAYER.with(() -> player),
                            new BloodPactSyncPacket(false)
                        );
                        player.sendStatusMessage(new StringTextComponent("\u00A7c\u041A\u0440\u043E\u0432\u0430\u0432\u044B\u0439 \u0414\u043E\u0433\u043E\u0432\u043E\u0440 \u0441\u043E\u0440\u0432\u0430\u043D! \u041D\u0443\u0436\u0435\u043D \u043E\u0442\u0434\u044B\u0445 (10 \u0441\u0435\u043A)."), true);
                    } else {
                        player.setHealth(Math.max(0.5F, player.getHealth() - drain));
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
