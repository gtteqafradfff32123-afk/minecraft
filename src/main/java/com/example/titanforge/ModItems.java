package com.example.titanforge;

import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TitanForge.MOD_ID);

    public static final RegistryObject<Item> SYRINGE = ITEMS.register("syringe",
        () -> new SyringeItem(new Item.Properties().group(ItemGroup.TOOLS).maxStackSize(16)));

    public static final RegistryObject<Item> YELLOW_DECAY_BLOCK = ITEMS.register("yellow_decay_block",
        () -> new BlockItem(ModBlocks.YELLOW_DECAY_BLOCK.get(),
            new Item.Properties().group(ItemGroup.DECORATIONS)));

    public static final RegistryObject<Item> RED_DECAY_BLOCK = ITEMS.register("red_decay_block",
        () -> new BlockItem(ModBlocks.RED_DECAY_BLOCK.get(),
            new Item.Properties().group(ItemGroup.DECORATIONS)));
}
