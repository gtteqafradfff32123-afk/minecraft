package com.example.titanforge;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

public final class TruthDissolveManager {
    private static final int MAX_BLOCKS = 9;
    private static final int COOLDOWN = 200;
    private static final List<Job> JOBS = new ArrayList<>();

    private static final class Job {
        final UUID owner;
        final ServerWorld world;
        final List<BlockPos> blocks;
        int index;
        int delay;

        Job(ServerPlayerEntity owner, List<BlockPos> blocks) {
            this.owner = owner.getUniqueID();
            this.world = (ServerWorld) owner.world;
            this.blocks = blocks;
        }
    }

    private TruthDissolveManager() {}

    public static boolean start(ServerPlayerEntity player, BlockPos origin) {
        long now = player.world.getGameTime();
        if (now < player.getPersistentData().getLong("TF_DissolveCooldown")) return false;
        ServerWorld world = (ServerWorld) player.world;
        if (!canDissolve(world, origin)) return false;

        List<BlockPos> selected = collect(world, origin);
        if (selected.isEmpty()) return false;
        player.getPersistentData().putLong("TF_DissolveCooldown", now + COOLDOWN);
        JOBS.add(new Job(player, selected));
        return true;
    }

    public static void tick(ServerWorld world) {
        Iterator<Job> it = JOBS.iterator();
        while (it.hasNext()) {
            Job job = it.next();
            if (job.world != world) continue;
            if (++job.delay < 3) continue;
            job.delay = 0;

            if (job.index >= job.blocks.size()) {
                it.remove();
                continue;
            }

            BlockPos pos = job.blocks.get(job.index++);
            if (!canDissolve(world, pos)) continue;
            world.spawnParticle(ParticleTypes.ASH, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    18, 0.35D, 0.35D, 0.35D, 0.03D);
            world.spawnParticle(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    8, 0.25D, 0.25D, 0.25D, 0.02D);
            world.playSound(null, pos, SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE,
                    SoundCategory.BLOCKS, 0.45F, 0.45F);
            world.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        }
    }

    private static List<BlockPos> collect(ServerWorld world, BlockPos origin) {
        List<BlockPos> result = new ArrayList<>();
        Queue<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        open.add(origin.toImmutable());

        while (!open.isEmpty() && result.size() < MAX_BLOCKS) {
            BlockPos pos = open.poll();
            if (!visited.add(pos) || !canDissolve(world, pos)) continue;
            result.add(pos);
            open.add(pos.up());
            open.add(pos.down());
            open.add(pos.north());
            open.add(pos.south());
            open.add(pos.east());
            open.add(pos.west());
        }
        return result;
    }

    public static boolean canDissolve(ServerWorld world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.isAir()) return false;
        if (state.getBlock() == Blocks.BEDROCK || state.getBlock() == Blocks.BARRIER
                || state.getBlock() == Blocks.END_PORTAL || state.getBlock() == Blocks.END_PORTAL_FRAME
                || state.getBlock() == Blocks.NETHER_PORTAL || state.getBlock() == Blocks.COMMAND_BLOCK
                || state.getBlock() == Blocks.CHAIN_COMMAND_BLOCK || state.getBlock() == Blocks.REPEATING_COMMAND_BLOCK)
            return false;
        if (state.getBlockHardness(world, pos) < 0.0F) return false;
        TileEntity tile = world.getTileEntity(pos);
        if (tile != null) return false;
        return !com.example.titanforge.liminal.LiminalManager.isProtectedWall(world, pos);
    }
}
