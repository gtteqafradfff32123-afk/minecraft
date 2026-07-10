package com.example.titanforge.backrooms;

import com.example.titanforge.TitanForge;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TitanForge.MOD_ID)
public final class BackroomsDebugCommand {
    private BackroomsDebugCommand() {}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("titanforge")
            .requires(source -> source.hasPermissionLevel(2))
            .then(Commands.literal("backrooms")
                .then(Commands.literal("enter")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().asPlayer();

                        if (BackroomsSessionManager.isInside(player.getUniqueID())) {
                            context.getSource().sendErrorMessage(
                                new StringTextComponent("Вы уже находитесь в Backrooms."));
                            return 0;
                        }

                        if (!BackroomsSessionManager.start(player, player)) {
                            context.getSource().sendErrorMessage(
                                new StringTextComponent("§cНе удалось запустить Backrooms-сессию."));
                            return 0;
                        }

                        return 1;
                    }))));
    }
}
