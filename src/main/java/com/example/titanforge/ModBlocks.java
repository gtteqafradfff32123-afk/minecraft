package com.example.titanforge;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, TitanForge.MOD_ID);

    public static final RegistryObject<Block> RED_DECAY_BLOCK = BLOCKS.register(
        "red_decay_block",
        () -> new DecayBlock(null,
            Block.Properties.create(Material.ROCK)
                .hardnessAndResistance(2.0F, 3.0F)
                .tickRandomly()));

    public static final RegistryObject<Block> YELLOW_DECAY_BLOCK = BLOCKS.register(
        "yellow_decay_block",
        () -> new DecayBlock(RED_DECAY_BLOCK,
            Block.Properties.create(Material.ROCK)
                .hardnessAndResistance(2.0F, 3.0F)
                .tickRandomly()));

    private ModBlocks() {}
}
