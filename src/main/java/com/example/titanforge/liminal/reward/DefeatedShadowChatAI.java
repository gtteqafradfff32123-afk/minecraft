package com.example.titanforge.liminal.reward;

import com.example.titanforge.TitanForge;
import com.example.titanforge.entities.ShadowEntity;
import com.example.titanforge.liminal.chat.LiminalChatAI;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;

import java.util.UUID;

public final class DefeatedShadowChatAI {
    public static boolean onChat(ServerPlayerEntity player, String message) {
        if (!isDefeatedShadowPresent(player)) return false;

        String reply = LiminalChatAI.getReply(player.getUniqueID(), message);
        if (reply != null && !reply.isEmpty()) {
            String name = player.getGameProfile().getName();
            player.sendMessage(
                new StringTextComponent("\u00A78[\u00A77\u0422\u0435\u043D\u044C " + name + "\u00A78]\u00A7f " + reply),
                player.getUniqueID());
        }
        return true;
    }

    private static boolean isDefeatedShadowPresent(ServerPlayerEntity player) {
        if (!(player.world instanceof ServerWorld)) return false;
        ServerWorld world = (ServerWorld) player.world;
        for (ShadowEntity shadow : world.getEntitiesWithinAABB(
                com.example.titanforge.entities.ShadowEntity.class,
                player.getBoundingBox().grow(20.0D),
                s -> DefeatedShadowTag.belongsTo(s, player))) {
            return true;
        }
        return false;
    }

    private DefeatedShadowChatAI() {}
}
