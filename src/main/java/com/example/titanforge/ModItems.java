package com.example.titanforge;

import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, TitanForge.MOD_ID);

    public static final RegistryObject<Item> SYRINGE = ITEMS.register("syringe",
        () -> new SyringeItem(new Item.Properties().group(ItemGroup.TOOLS).maxStackSize(16)));


}
