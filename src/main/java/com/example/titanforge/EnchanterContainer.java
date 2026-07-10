package com.example.titanforge;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;

public class EnchanterContainer extends Container {

    public EnchanterContainer(int id, PlayerInventory inv, PacketBuffer data) {
        this(id, inv);
    }

    public EnchanterContainer(int id, PlayerInventory inv) {
        super(ModContainers.ENCHANTER.get(), id);
    }

    @Override
    public boolean canInteractWith(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack transferStackInSlot(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }
}
