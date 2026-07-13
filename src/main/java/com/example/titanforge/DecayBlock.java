package com.example.titanforge;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Random;
import java.util.function.Supplier;

public final class DecayBlock extends Block {
    private final Supplier<Block> nextStage;
    private final int minDelay;
    private final int delaySpread;

    public DecayBlock(Supplier<Block> nextStage,
                      int minDelay,
                      int delaySpread,
                      Properties properties) {
        super(properties);
        this.nextStage = nextStage;
        this.minDelay = minDelay;
        this.delaySpread = delaySpread;
    }

    @Override
    public void onBlockAdded(BlockState state, World world,
                             BlockPos pos, BlockState oldState,
                             boolean moving) {
        super.onBlockAdded(state, world, pos, oldState, moving);
        if (world.isRemote) return;
        schedule((ServerWorld) world, pos);
    }

    private void schedule(ServerWorld world, BlockPos pos) {
        int delay = minDelay +
            (delaySpread <= 0 ? 0 : world.rand.nextInt(delaySpread + 1));
        world.getPendingBlockTicks().scheduleTick(pos, this, delay);
    }

    @Override
    public void tick(BlockState state, ServerWorld world,
                     BlockPos pos, Random random) {
        if (world.getBlockState(pos).getBlock() != this) return;

        if (nextStage == null) {
            world.removeBlock(pos, false);
        } else {
            world.setBlockState(
                pos, nextStage.get().getDefaultState(), 18);
        }
    }
}
