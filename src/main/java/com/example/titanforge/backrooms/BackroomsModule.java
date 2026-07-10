package com.example.titanforge.backrooms;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.List;

public abstract class BackroomsModule {
    protected final Direction facing;
    protected final int width;
    protected final int depth;
    protected final int height;
    private final List<ModuleSocket> sockets = new ArrayList<>();
    private static final class DelayedBlock {
        final int x, y, z;
        final BlockState state;
        DelayedBlock(int x, int y, int z, BlockState state) {
            this.x = x; this.y = y; this.z = z; this.state = state;
        }
    }
    private final List<DelayedBlock> blocks = new ArrayList<>();
    private final List<BlockPos> doors = new ArrayList<>();
    private final List<int[]> socketRel = new ArrayList<>();
    private final List<Direction> socketDirs = new ArrayList<>();

    public BackroomsModule(Direction facing, int width, int depth, int height) {
        this.facing = facing;
        this.width = width;
        this.depth = depth;
        this.height = height;
        design();
    }

    public abstract BackroomsModuleType type();

    protected abstract void design();

    protected final void shell(BlockState floor, BlockState wall, BlockState ceiling) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                for (int y = 0; y < height; y++) {
                    boolean edge = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
                    if (y == 0) {
                        block(x, 0, z, floor);
                    } else if (y == height - 1) {
                        block(x, height - 1, z, ceiling);
                    } else if (edge) {
                        block(x, y, z, wall);
                    }
                }
            }
        }
    }

    protected final void block(int x, int y, int z, BlockState state) {
        blocks.add(new DelayedBlock(x, y, z, state));
    }

    protected final void clearDoor(int x, int y, int z) {
        doors.add(new BlockPos(x, y, z));
    }

    protected final void socket(int x, int y, int z, Direction dir) {
        socketRel.add(new int[]{x, y, z});
        socketDirs.add(dir);
    }

    public void place(ServerWorld world, BlockPos center) {
        int ox = center.getX() - width / 2;
        int oy = center.getY();
        int oz = center.getZ() - depth / 2;
        for (DelayedBlock db : blocks) {
            world.setBlockState(new BlockPos(ox + db.x, oy + db.y, oz + db.z), db.state, 3);
        }
        for (BlockPos door : doors) {
            BlockPos wp = new BlockPos(ox + door.getX(), oy + door.getY(), oz + door.getZ());
            world.setBlockState(wp, Blocks.AIR.getDefaultState(), 3);
            world.setBlockState(wp.up(), Blocks.AIR.getDefaultState(), 3);
        }
        sockets.clear();
        for (int i = 0; i < socketRel.size(); i++) {
            int[] r = socketRel.get(i);
            sockets.add(new ModuleSocket(new BlockPos(ox + r[0], oy + r[1], oz + r[2]), socketDirs.get(i)));
        }
    }

    public List<ModuleSocket> sockets() {
        return sockets;
    }

    public List<BlockPos> getDoors() {
        return doors;
    }
}
