package com.example.titanforge.liminal.copy;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;

public final class DeltaCopier {
    public static void clearChunk(ServerWorld target, ChunkPos pos,
                                  BlockPos center, int radius) {
        int radiusSq = radius * radius;

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = pos.getXStart() + lx;
                int wz = pos.getZStart() + lz;
                int dx = wx - center.getX();
                int dz = wz - center.getZ();
                if (dx * dx + dz * dz > radiusSq) continue;

                for (int y = 0; y < 256; y++) {
                    BlockPos wp = new BlockPos(wx, y, wz);
                    TileEntity tile = target.getTileEntity(wp);
                    if (tile != null) target.removeTileEntity(wp);
                    target.setBlockState(wp, Blocks.AIR.getDefaultState(), 18);
                }
            }
        }
    }

    public static int copyChunkFull(ServerWorld source,
                                    ServerWorld target,
                                    ChunkPos sourceChunk,
                                    BlockPos sourceCenter,
                                    BlockPos targetCenter,
                                    int radius) {
        source.getChunk(sourceChunk.x, sourceChunk.z);

        int copied = 0;
        int radiusSq = radius * radius;

        for (int localX = 0; localX < 16; localX++) {
            int sourceX = sourceChunk.getXStart() + localX;
            int dx = sourceX - sourceCenter.getX();

            for (int localZ = 0; localZ < 16; localZ++) {
                int sourceZ = sourceChunk.getZStart() + localZ;
                int dz = sourceZ - sourceCenter.getZ();
                if (dx * dx + dz * dz > radiusSq) continue;

                for (int y = 0; y < 256; y++) {
                    int targetX = targetCenter.getX() + dx;
                    int targetY = y;
                    int targetZ = targetCenter.getZ() + dz;

                    BlockState state = source.getBlockState(new BlockPos(sourceX, y, sourceZ));

                    if (state.getBlock() == Blocks.BEDROCK) {
                        state = Blocks.AIR.getDefaultState();
                    }

                    BlockPos targetPos = new BlockPos(targetX, targetY, targetZ);
                    TileEntity old = target.getTileEntity(targetPos);
                    if (old != null) target.removeTileEntity(targetPos);

                    target.setBlockState(targetPos, state, 18);
                    if (!state.isAir()) copied++;

                    if (state.hasTileEntity()) {
                        TileEntity sourceTile = source.getTileEntity(new BlockPos(sourceX, y, sourceZ));
                        TileEntity targetTile = target.getTileEntity(targetPos);
                        if (sourceTile != null && targetTile != null) {
                            CompoundNBT nbt = sourceTile.write(new CompoundNBT());
                            nbt.remove("Items");
                            nbt.remove("LootTable");
                            nbt.remove("LootTableSeed");
                            targetTile.read(state, nbt);
                            targetTile.markDirty();
                        }
                    }
                }
            }
        }
        return copied;
    }

    public static void buildFloor(ServerWorld world, BlockPos center, int radius) {
        int floorY = Math.max(0, center.getY() - 41);
        int radiusSq = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z > radiusSq) continue;
                world.setBlockState(center.add(x, floorY - center.getY(), z),
                        Blocks.BEDROCK.getDefaultState(), 18);
            }
        }
    }

    private DeltaCopier() {}
}
