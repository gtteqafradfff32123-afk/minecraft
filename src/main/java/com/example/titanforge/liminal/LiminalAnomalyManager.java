package com.example.titanforge.liminal;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LiminalAnomalyManager {
    private static final Map<UUID, PlayerAnomalies> STATES = new HashMap<>();

    private static final class TempBlock {
        final BlockPos pos;
        final BlockState oldState;
        int life;
        TempBlock(BlockPos pos, BlockState oldState, int life) {
            this.pos = pos;
            this.oldState = oldState;
            this.life = life;
        }
    }

    private static final class PlayerAnomalies {
        int timer;
        int loopCount;
        BlockPos fakeExit;
        BlockPos loopDoor;
        final List<TempBlock> temporary = new ArrayList<>();
    }

    private LiminalAnomalyManager() {}

    public static void tick(ServerWorld world, ServerPlayerEntity player, LiminalManager.State liminal) {
        PlayerAnomalies state = STATES.computeIfAbsent(player.getUniqueID(), id -> new PlayerAnomalies());
        state.timer++;
        tickTemporary(world, state);

        int intensity = liminal.anomalyIntensity;
        int footprintsInterval = Math.max(40, 160 - intensity * 10);
        int falseExitInterval = Math.max(200, 700 - intensity * 40);
        int writingInterval = Math.max(160, 900 - intensity * 55);
        int roomInterval = Math.max(250, 1200 - intensity * 70);

        if (state.timer % footprintsInterval == 0) spawnFootprints(world, player, liminal);
        if (state.timer % falseExitInterval == 0 && state.fakeExit == null) spawnFalseExit(world, player, liminal, state);
        if (state.timer % writingInterval == 0) spawnDisappearingWriting(world, player, liminal, state);
        if (state.timer % roomInterval == 0) spawnErroneousRoom(world, player, liminal, state);
    }

    public static boolean useDoor(ServerWorld world, ServerPlayerEntity player, BlockPos door,
                                  LiminalManager.State liminal) {
        PlayerAnomalies state = STATES.computeIfAbsent(player.getUniqueID(), id -> new PlayerAnomalies());
        if (state.fakeExit != null && state.fakeExit.distanceSq(door) <= 4.0D) {
            teleportInside(world, player, liminal, 35.0D, 70.0D);
            removeFalseExit(world, state);
            return true;
        }

        if (state.loopDoor == null) state.loopDoor = door.toImmutable();
        if (state.loopDoor.equals(door) && state.loopCount < 3) {
            state.loopCount++;
            Vector3d look = player.getLookVec().normalize();
            double x = player.getPosX() - look.x * (8.0D + state.loopCount * 2.0D);
            double z = player.getPosZ() - look.z * (8.0D + state.loopCount * 2.0D);
            BlockPos safe = findGround(world, new BlockPos(x, player.getPosY(), z));
            player.setPositionAndUpdate(safe.getX() + 0.5D, safe.getY() + 1.0D, safe.getZ() + 0.5D);
            world.spawnParticle(ParticleTypes.PORTAL, player.getPosX(), player.getPosY() + 1.0D,
                    player.getPosZ(), 24, 0.4D, 0.8D, 0.4D, 0.12D);
            return true;
        }
        return false;
    }

    public static void clear(ServerWorld world, UUID player) {
        PlayerAnomalies state = STATES.remove(player);
        if (state == null) return;
        for (TempBlock temp : state.temporary) {
            if (world.getBlockState(temp.pos).getBlock() != Blocks.BLACK_CONCRETE)
                world.setBlockState(temp.pos, temp.oldState, 3);
        }
        removeFalseExit(world, state);
    }

    private static void spawnFootprints(ServerWorld world,
                                        ServerPlayerEntity player,
                                        LiminalManager.State state) {
        Vector3d forward = player.getMotion().mul(1.0D, 0.0D, 1.0D);
        if (forward.lengthSquared() < 0.01D) {
            forward = player.getLookVec().mul(1.0D, 0.0D, 1.0D);
        }
        forward = forward.normalize();
        Vector3d side = new Vector3d(-forward.z, 0.0D, forward.x);

        for (int i = 2; i <= 9; i++) {
            Vector3d foot = player.getPositionVec()
                .add(forward.scale(i * 1.25D))
                .add(side.scale((i & 1) == 0 ? 0.28D : -0.28D));

            BlockPos ground = findGround(world,
                new BlockPos(foot.x, player.getPosY() + 4.0D, foot.z));

            world.spawnParticle(
                player,
                ParticleTypes.SOUL,
                true,
                ground.getX() + 0.5D,
                ground.getY() + 1.03D,
                ground.getZ() + 0.5D,
                5,
                0.07D, 0.01D, 0.07D,
                0.0D);
        }
    }

    private static void spawnFalseExit(ServerWorld world, ServerPlayerEntity player,
                                       LiminalManager.State liminal, PlayerAnomalies state) {
        BlockPos base = randomInside(world, liminal, player, 25.0D, 55.0D);
        if (base == null) return;
        Direction facing = player.getHorizontalFacing().getOpposite();
        BlockPos left = base.offset(facing.rotateY());
        BlockPos right = base.offset(facing.rotateYCCW());
        BlockPos top = base.up(2);
        placeTemp(world, state, left, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 1200);
        placeTemp(world, state, right, Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState(), 1200);
        placeTemp(world, state, top, Blocks.SEA_LANTERN.getDefaultState(), 1200);
        placeTemp(world, state, base, Blocks.DARK_OAK_DOOR.getDefaultState()
                .with(DoorBlock.FACING, facing), 1200);
        placeTemp(world, state, base.up(), Blocks.DARK_OAK_DOOR.getDefaultState()
                .with(DoorBlock.FACING, facing).with(DoorBlock.HALF,
                        net.minecraft.state.properties.DoubleBlockHalf.UPPER), 1200);
        state.fakeExit = base;
    }

    private static void spawnDisappearingWriting(ServerWorld world, ServerPlayerEntity player,
                                                    LiminalManager.State liminal, PlayerAnomalies state) {
        BlockPos pos = randomInside(world, liminal, player, 8.0D, 18.0D);
        if (pos == null) return;
        placeTemp(world, state, pos, Blocks.REDSTONE_TORCH.getDefaultState(), 160);
        player.sendStatusMessage(new net.minecraft.util.text.StringTextComponent("§8ТЫ УЖЕ БЫЛ ЗДЕСЬ"), true);
    }

    private static void spawnErroneousRoom(ServerWorld world, ServerPlayerEntity player,
                                            LiminalManager.State liminal, PlayerAnomalies state) {
        BlockPos center = randomInside(world, liminal, player, 25.0D, 45.0D);
        if (center == null) return;
        int budget = 90;
        for (int x = -3; x <= 3 && budget > 0; x++) {
            for (int z = -3; z <= 3 && budget > 0; z++) {
                for (int y = 0; y <= 3 && budget > 0; y++) {
                    boolean wall = Math.abs(x) == 3 || Math.abs(z) == 3 || y == 0 || y == 3;
                    if (!wall) continue;
                    BlockPos pos = center.add(x, y, z);
                    if (LiminalManager.isProtectedWall(world, pos) || world.getTileEntity(pos) != null) continue;
                    BlockState wrong = ((x + z + y) % 11 == 0)
                            ? Blocks.MOSSY_COBBLESTONE.getDefaultState()
                            : Blocks.STONE_BRICKS.getDefaultState();
                    placeTemp(world, state, pos, wrong, 1000);
                    budget--;
                }
            }
        }
    }

    private static void placeTemp(ServerWorld world, PlayerAnomalies state, BlockPos pos,
                                  BlockState replacement, int life) {
        if (LiminalManager.isProtectedWall(world, pos) || world.getTileEntity(pos) != null) return;
        state.temporary.add(new TempBlock(pos.toImmutable(), world.getBlockState(pos), life));
        world.setBlockState(pos, replacement, 3);
    }

    private static void tickTemporary(ServerWorld world, PlayerAnomalies state) {
        Iterator<TempBlock> it = state.temporary.iterator();
        while (it.hasNext()) {
            TempBlock temp = it.next();
            if (--temp.life > 0) continue;
            if (!LiminalManager.isProtectedWall(world, temp.pos))
                world.setBlockState(temp.pos, temp.oldState, 3);
            it.remove();
        }
    }

    private static void removeFalseExit(ServerWorld world, PlayerAnomalies state) {
        state.fakeExit = null;
    }

    private static void teleportInside(ServerWorld world, ServerPlayerEntity player,
                                       LiminalManager.State liminal, double min, double max) {
        BlockPos pos = randomInside(world, liminal, player, min, max);
        if (pos != null) player.setPositionAndUpdate(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
    }

    private static BlockPos randomInside(ServerWorld world, LiminalManager.State state,
                                         ServerPlayerEntity player, double min, double max) {
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = world.rand.nextDouble() * Math.PI * 2.0D;
            double distance = min + world.rand.nextDouble() * (max - min);
            double x = player.getPosX() + Math.cos(angle) * distance;
            double z = player.getPosZ() + Math.sin(angle) * distance;
            if (!inside(state, x, z, 5.0D)) continue;
            return findGround(world, new BlockPos(x, player.getPosY(), z));
        }
        return null;
    }

    private static boolean inside(LiminalManager.State state, double x, double z, double margin) {
        double dx = x - state.center.getX();
        double dz = z - state.center.getZ();
        return dx * dx + dz * dz < (100.0D - margin) * (100.0D - margin);
    }

    private static BlockPos findGround(ServerWorld world, BlockPos origin) {
        BlockPos.Mutable pos = new BlockPos.Mutable(origin.getX(), Math.min(250, origin.getY() + 8), origin.getZ());
        while (pos.getY() > 1 && world.isAirBlock(pos)) pos.move(Direction.DOWN);
        return pos.toImmutable();
    }
}
