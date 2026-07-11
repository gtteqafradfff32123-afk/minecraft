package com.example.titanforge.backrooms;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.WorldSavedData;
import java.util.*;

public final class BackroomsSavedData extends WorldSavedData {
    public static final String NAME = "titanforge_backrooms";

    public static final class ReturnPoint {
        public final String dim;
        public final BlockPos pos;

        ReturnPoint(String d, BlockPos p) {
            dim = d;
            pos = p;
        }
    }

    private final Map<UUID, ReturnPoint> points = new HashMap<>();

    public BackroomsSavedData() {
        super(NAME);
    }

    public void put(UUID id, String dim, BlockPos p) {
        points.put(id, new ReturnPoint(dim, p.toImmutable()));
        markDirty();
    }

    public ReturnPoint remove(UUID id) {
        ReturnPoint r = points.remove(id);
        markDirty();
        return r;
    }

    @Override
    public void read(CompoundNBT n) {
        points.clear();
        ListNBT list = n.getList("players", 10);
        for (int i = 0; i < list.size(); i++) {
            CompoundNBT e = list.getCompound(i);
            points.put(e.getUniqueId("id"),
                    new ReturnPoint(e.getString("dim"),
                            BlockPos.fromLong(e.getLong("pos"))));
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT n) {
        ListNBT list = new ListNBT();
        for (Map.Entry<UUID, ReturnPoint> e : points.entrySet()) {
            CompoundNBT x = new CompoundNBT();
            x.putUniqueId("id", e.getKey());
            x.putString("dim", e.getValue().dim);
            x.putLong("pos", e.getValue().pos.toLong());
            list.add(x);
        }
        n.put("players", list);
        return n;
    }
}
