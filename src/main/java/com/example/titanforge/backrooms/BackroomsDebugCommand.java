package com.example.titanforge.backrooms;

import com.example.titanforge.TitanForge;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

public final class BackroomsDebugCommand {
    private BackroomsDebugCommand() {}

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("titanforge")
                .then(Commands.literal("backrooms")
                        .then(Commands.literal("enter")
                                .requires(s -> s.hasPermissionLevel(2))
                                .executes(BackroomsDebugCommand::enter)
                        )
                        .then(Commands.literal("leave")
                                .requires(s -> s.hasPermissionLevel(2))
                                .executes(BackroomsDebugCommand::leave)
                        )
                )
        );
    }

    private static int enter(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.asPlayer();
        if (BackroomsSessionManager.start(player, player)) {
            source.sendFeedback(new StringTextComponent("§aВход в Backrooms"), true);
            return 1;
        } else {
            source.sendErrorMessage(new StringTextComponent("§cОшибка входа в Backrooms"));
            return 0;
        }
    }

    private static int leave(CommandContext<CommandSource> ctx) throws CommandSyntaxException {
        CommandSource source = ctx.getSource();
        ServerPlayerEntity player = source.asPlayer();
        if (BackroomsSessionManager.isInBackrooms(player)) {
            BackroomsSessionManager.finish(player, false);
            source.sendFeedback(new StringTextComponent("§aВыход из Backrooms"), true);
            return 1;
        } else {
            source.sendErrorMessage(new StringTextComponent("§cТы не в Backrooms"));
            return 0;
        }
    }
}
