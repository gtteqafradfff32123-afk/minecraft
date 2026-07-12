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
    private static int minY(BlockPos center) { return Math.max(1, center.getY() - 40); }
    private static int maxY(BlockPos center) { return Math.min(255, center.getY() + 40); }

    public static void clearChunk(ServerWorld target, ChunkPos pos,
                                  BlockPos center, int radius) {
        int radiusSq = radius * radius;
        int minY = minY(center);
        int maxY = maxY(center);

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = pos.getXStart() + lx;
                int wz = pos.getZStart() + lz;
                int dx = wx - center.getX();
                int dz = wz - center.getZ();
                if (dx * dx + dz * dz > radiusSq) continue;

                for (int y = minY; y <= maxY; y++) {
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
        int minSourceY = Math.max(1, sourceCenter.getY() - 40);
        int maxSourceY = Math.min(255, sourceCenter.getY() + 40);

        BlockPos.Mutable sourcePos = new BlockPos.Mutable();
        BlockPos.Mutable targetPos = new BlockPos.Mutable();

        for (int localX = 0; localX < 16; localX++) {
            int sourceX = sourceChunk.getXStart() + localX;
            int dx = sourceX - sourceCenter.getX();

            for (int localZ = 0; localZ < 16; localZ++) {
                int sourceZ = sourceChunk.getZStart() + localZ;
                int dz = sourceZ - sourceCenter.getZ();
                if (dx * dx + dz * dz > radiusSq) continue;

                for (int sourceY = minSourceY;
                     sourceY <= maxSourceY;
                     sourceY++) {
                    int dy = sourceY - sourceCenter.getY();
                    int targetX = targetCenter.getX() + dx;
                    int targetY = targetCenter.getY() + dy;
                    int targetZ = targetCenter.getZ() + dz;
                    if (targetY < 1 || targetY > 255) continue;

                    sourcePos.setPos(sourceX, sourceY, sourceZ);
                    targetPos.setPos(targetX, targetY, targetZ);

                    BlockState state = source.getBlockState(sourcePos);
                    TileEntity old = target.getTileEntity(targetPos);
                    if (old != null) target.removeTileEntity(targetPos);

                    target.setBlockState(targetPos, state, 18);
                    copied++;

                    if (state.hasTileEntity()) {
                        TileEntity sourceTile = source.getTileEntity(sourcePos);
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
