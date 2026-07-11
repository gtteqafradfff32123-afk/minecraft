package com.example.titanforge.backrooms.modules;

import com.example.titanforge.backrooms.*;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;

public final class LobbyModule extends BackroomsModule {
    public LobbyModule(Direction facing) {
        super(facing, 14, 11, 6);
    }

    @Override
    public BackroomsModuleType type() {
        return BackroomsModuleType.LOBBY;
    }

    @Override
    protected void design() {
        standardShell();

        for (int x = 3; x < width - 2; x += 4) {
            for (int z = 2; z < length - 2; z++) {
                if (z == length / 2
                        || z == length / 2 + 1) {
                    continue;
                }
                for (int y = 1; y <= 3; y++) {
                    block(
                            x,
                            y,
                            z,
                            Blocks.STRIPPED_OAK_LOG
                                    .getDefaultState());
                }
            }
        }

        clearDoor(width / 2 - 1, 1, 0);
        clearDoor(
                width / 2 - 1,
                1,
                length - 1);
        clearDoor(0, 1, length / 2 - 1);
        clearDoor(
                width - 2,
                1,
                length / 2 - 1);

        socket(
                width / 2,
                1,
                0,
                Direction.NORTH);
        socket(
                width / 2,
                1,
                length - 1,
                Direction.SOUTH);
        socket(
                0,
                1,
                length / 2,
                Direction.WEST);
        socket(
                width - 1,
                1,
                length / 2,
                Direction.EAST);
    }
}
