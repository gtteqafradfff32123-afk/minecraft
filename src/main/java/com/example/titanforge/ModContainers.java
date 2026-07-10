package com.example.titanforge;

import net.minecraft.inventory.container.ContainerType;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ModContainers {
    public static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, TitanForge.MOD_ID);

    public static final RegistryObject<ContainerType<EnchanterContainer>> ENCHANTER = CONTAINERS.register("enchanter",
            () -> new ContainerType<>(EnchanterContainer::new));
}
