package com.example.titanforge.backrooms;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RotatedPillarBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import java.util.*;

public abstract class BackroomsModule {
    protected final Direction facing;
    protected final int width, length, height;
    protected final List<Placement> placements = new ArrayList<>();
    protected final List<ModuleSocket> sockets = new ArrayList<>();
    private BlockPos activeCenter;
    private int cursor;

    protected BackroomsModule(
            Direction facing,
            int width,
            int length,
            int height) {
        this.facing = facing;
        this.width = width;
        this.length = length;
        this.height = height;
        design();
    }

    protected abstract void design();
    public abstract BackroomsModuleType type();

    public Direction facing() {
        return facing;
    }

    public List<ModuleSocket> sockets() {
        return Collections.unmodifiableList(sockets);
    }

    public int connectionOffset() {
        return Math.max(width, length) / 2 + 2;
    }

    public boolean buildStep(
            ServerWorld world,
            BlockPos center,
            int budget) {
        if (activeCenter == null
                || !activeCenter.equals(center)) {
            activeCenter = center.toImmutable();
            cursor = 0;
        }

        int end = Math.min(
                placements.size(),
                cursor + Math.max(1, budget));

        while (cursor < end) {
            Placement placement = placements.get(cursor++);
            BlockPos worldPos = center.add(
                    rotateLocal(
                            placement.localPos,
                            facing));
            world.setBlockState(
                    worldPos,
                    placement.state,
                    18);
        }
        return cursor >= placements.size();
    }

    public AxisAlignedBB boundsAt(BlockPos center) {
        int halfW = width / 2;
        int halfL = length / 2;
        return new AxisAlignedBB(
                center.add(-halfW, 0, -halfL),
                center.add(
                        width - halfW,
                        height,
                        length - halfL));
    }

    protected void block(
            int x,
            int y,
            int z,
            BlockState state) {
        placements.add(new Placement(
                new BlockPos(
                        x - width / 2,
                        y,
                        z - length / 2),
                state));
    }

    protected void socket(
            int x,
            int y,
            int z,
            Direction localFacing) {
        sockets.add(new ModuleSocket(
                new BlockPos(
                        x - width / 2,
                        y,
                        z - length / 2),
                localFacing));
    }

    protected void clearDoor(
            int x,
            int y,
            int z) {
        for (int dx = 0; dx < 2; dx++) {
            for (int dy = 0; dy < 3; dy++) {
                block(
                        x + dx,
                        y + dy,
                        z,
                        Blocks.AIR.getDefaultState());
            }
        }
    }

    protected void standardShell() {
        BlockState floor =
                Blocks.GRAY_WOOL.getDefaultState();
        BlockState wall =
                Blocks.STRIPPED_OAK_LOG
                        .getDefaultState()
                        .with(
                                RotatedPillarBlock.AXIS,
                                Direction.Axis.Y);
        BlockState ceiling =
                Blocks.SMOOTH_STONE.getDefaultState();

        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                block(x, 0, z, floor);
                block(x, height - 1, z, ceiling);
            }
        }

        for (int y = 1; y < height - 1; y++) {
            for (int x = 0; x < width; x++) {
                block(x, y, 0, wall);
                block(x, y, length - 1, wall);
            }
            for (int z = 1; z < length - 1; z++) {
                block(0, y, z, wall);
                block(width - 1, y, z, wall);
            }
        }
        lightingGrid();
    }

    protected void ceilingLamp(int x, int z) {
        block(
                x,
                height - 1,
                z,
                Blocks.SEA_LANTERN.getDefaultState());
        block(
                x,
                height - 2,
                z,
                Blocks.IRON_TRAPDOOR.getDefaultState());
    }

    protected void lightingGrid() {
        boolean longOnX = width >= length;
        int longSize = longOnX ? width : length;
        int shortSize = longOnX ? length : width;
        int rowA = Math.max(2, shortSize / 3);
        int rowB = Math.min(
                shortSize - 3,
                shortSize * 2 / 3);

        for (int p = 3; p < longSize - 2; p += 5) {
            if (longOnX) {
                ceilingLamp(p, rowA);
                if (rowB != rowA) ceilingLamp(p, rowB);
            } else {
                ceilingLamp(rowA, p);
                if (rowB != rowA) ceilingLamp(rowB, p);
            }
        }
    }

    public static BlockPos rotateLocal(
            BlockPos local,
            Direction facing) {
        int x = local.getX();
        int y = local.getY();
        int z = local.getZ();
        switch (facing) {
            case SOUTH:
                return new BlockPos(-x, y, -z);
            case EAST:
                return new BlockPos(-z, y, x);
            case WEST:
                return new BlockPos(z, y, -x);
            case NORTH:
            default:
                return local;
        }
    }

    public static Direction rotateDirection(
            Direction local,
            Direction moduleFacing) {
        if (moduleFacing == Direction.NORTH) return local;
        if (moduleFacing == Direction.SOUTH) {
            return local.getOpposite();
        }
        if (moduleFacing == Direction.EAST) {
            return local.rotateY();
        }
        return local.rotateYCCW();
    }

    protected static final class Placement {
        final BlockPos localPos;
        final BlockState state;

        Placement(BlockPos pos, BlockState state) {
            this.localPos = pos.toImmutable();
            this.state = state;
        }
    }
}
