package com.example.titanforge.liminal.copy;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.nbt.CompoundNBT;

public class DeltaCopier {

    public static int copyChunkFull(ServerWorld source, ServerWorld target, ChunkPos pos, BlockPos center, int radius) {
        Chunk src = source.getChunk(pos.x, pos.z);
        int copied = 0;
        int r2 = radius * radius;
        int minY = Math.max(1, center.getY() - 40);
        int maxY = Math.min(255, center.getY() + 40);

        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int wx = pos.getXStart() + lx;
                int wz = pos.getZStart() + lz;
                double dx = wx - center.getX(), dz = wz - center.getZ();
                if (dx*dx + dz*dz > r2) continue;

                for (int y = minY; y <= maxY; y++) {
                    BlockState st = src.getBlockState(new BlockPos(lx, y, lz));
                    if (st.isAir()) continue;
                    BlockPos wp = new BlockPos(wx, y, wz);
                    target.setBlockState(wp, st, 18);
                    copied++;
                    if (st.hasTileEntity()) {
                        TileEntity te = source.getTileEntity(wp);
                        if (te != null) {
                            CompoundNBT nbt = te.write(new CompoundNBT());
                            nbt.remove("Items");
                            nbt.remove("LootTable");
                            nbt.remove("Inventory");
                            TileEntity dte = target.getTileEntity(wp);
                            if (dte != null) dte.read(st, nbt);
                        }
                    }
                }
            }
        }
        return copied;
    }

    public static void buildFloor(ServerWorld clone, BlockPos center, int radius) {
        int floorY = Math.max(0, center.getY() - 41);
        int r2 = radius * radius;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x*x + z*z > r2) continue;
                clone.setBlockState(new BlockPos(center.getX() + x, floorY, center.getZ() + z),
                    Blocks.BEDROCK.getDefaultState(), 18);
            }
        }
    }
}
