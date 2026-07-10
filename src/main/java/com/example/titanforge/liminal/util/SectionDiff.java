package com.example.titanforge.liminal.util;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.ChunkSection;

public class SectionDiff {

    public static boolean sectionsIdentical(ChunkSection a, ChunkSection b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        PacketBuffer bufA = new PacketBuffer(Unpooled.buffer());
        PacketBuffer bufB = new PacketBuffer(Unpooled.buffer());
        a.getData().write(bufA);
        b.getData().write(bufB);
        boolean same = bufA.equals(bufB);
        bufA.release(); bufB.release();
        return same;
    }
}
