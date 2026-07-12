package com.example.titanforge;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.Random;
import java.util.function.Supplier;

public final class DecayBlock extends Block {
    private final Supplier<Block> nextStage;

    public DecayBlock(Supplier<Block> nextStage, Properties properties) {
        super(properties);
        this.nextStage = nextStage;
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (nextStage == null) {
            world.removeBlock(pos, false);
        } else {
            world.setBlockState(pos, nextStage.get().getDefaultState(), 2);
        }
    }
}
