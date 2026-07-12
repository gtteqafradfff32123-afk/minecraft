package com.example.titanforge.liminal.reward;

import com.example.titanforge.entities.ShadowEntity;
import com.example.titanforge.liminal.LiminalManager;
import com.example.titanforge.liminal.chat.LiminalChatAI;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;

import java.util.UUID;

public final class DefeatedShadowChatAI {
    private DefeatedShadowChatAI() {}

    public static void reply(ServerPlayerEntity player, String message, String situation) {
        UUID id = player.getUniqueID();
        String prompt =
            "\u041E\u0442\u0432\u0435\u0447\u0430\u0439 \u0442\u043E\u043B\u044C\u043A\u043E \u043F\u043E-\u0440\u0443\u0441\u0441\u043A\u0438, \u043E\u0434\u043D\u043E\u0439 \u0438\u043B\u0438 \u0434\u0432\u0443\u043C\u044F \u043A\u043E\u0440\u043E\u0442\u043A\u0438\u043C\u0438 \u0444\u0440\u0430\u0437\u0430\u043C\u0438. "
            + "\u0422\u044B \u0422\u0435\u043D\u044C \u0438\u0433\u0440\u043E\u043A\u0430 " + player.getName().getString() + ". "
            + "\u0422\u044B \u043F\u0440\u043E\u0438\u0433\u0440\u0430\u043B\u0430 \u0435\u043C\u0443 \u0447\u0435\u0441\u0442\u043D\u044B\u0439 \u0431\u043E\u0439 \u0438 \u043F\u043E\u043D\u0438\u043C\u0430\u0435\u0448\u044C, \u0447\u0442\u043E \u043F\u0440\u043E\u0438\u0433\u0440\u0430\u043B\u0430. "
            + "\u0422\u044B \u0431\u043E\u043B\u044C\u0448\u0435 \u043D\u0435 \u0443\u0433\u0440\u043E\u0436\u0430\u0435\u0448\u044C, \u0447\u0442\u043E \u043E\u043D \u043D\u0435 \u0432\u044B\u0439\u0434\u0435\u0442, \u0438 \u043D\u0435 \u0437\u0430\u044F\u0432\u043B\u044F\u0435\u0448\u044C \u043E \u0441\u0432\u043E\u0435\u0439 \u043F\u043E\u0431\u0435\u0434\u0435. "
            + "\u0413\u043E\u0432\u043E\u0440\u0438 \u043C\u0440\u0430\u0447\u043D\u043E, \u0441\u043F\u043E\u043A\u043E\u0439\u043D\u043E \u0438 \u0441 \u0443\u044F\u0437\u0432\u0451\u043D\u043D\u043E\u0439 \u0433\u043E\u0440\u0434\u043E\u0441\u0442\u044C\u044E. "
            + "\u0422\u044B \u043E\u0445\u0440\u0430\u043D\u044F\u0435\u0448\u044C \u0434\u0432\u0430 \u0441\u0443\u043D\u0434\u0443\u043A\u0430 \u043D\u0430\u0433\u0440\u0430\u0434\u044B. "
            + "\u0415\u0441\u043B\u0438 \u0438\u0433\u0440\u043E\u043A \u043F\u0440\u0438\u043A\u0430\u0437\u044B\u0432\u0430\u0435\u0442 \u0441\u043B\u0435\u0434\u043E\u0432\u0430\u0442\u044C, \u043E\u0441\u0442\u0430\u0442\u044C\u0441\u044F \u0438\u043B\u0438 \u0432\u0435\u0440\u043D\u0443\u0442\u044C\u0441\u044F, \u043F\u0440\u0438\u0437\u043D\u0430\u0439 \u043A\u043E\u043C\u0430\u043D\u0434\u0443. "
            + "\u041D\u0438\u043A\u043E\u0433\u0434\u0430 \u043D\u0435 \u043D\u0430\u0437\u044B\u0432\u0430\u0439 \u0441\u0435\u0431\u044F \u0418\u0418. \u0421\u0438\u0442\u0443\u0430\u0446\u0438\u044F: " + situation;

        com.example.titanforge.liminal.ai.GrokClient.ask(prompt, java.util.Collections.emptyList(), message)
            .thenAccept(answer -> player.server.execute(() -> {
                ServerPlayerEntity current = player.server.getPlayerList()
                    .getPlayerByUUID(id);
                if (current == null) return;
                String text = answer == null || answer.trim().isEmpty()
                    ? "\u042F \u0443\u0441\u043B\u044B\u0448\u0430\u043B. \u041F\u043E\u0441\u043B\u0435 \u043F\u043E\u0440\u0430\u0436\u0435\u043D\u0438\u044F \u043E\u0441\u0442\u0430\u0451\u0442\u0441\u044F \u0442\u043E\u043B\u044C\u043A\u043E \u0432\u044B\u043F\u043E\u043B\u043D\u044F\u0442\u044C \u043E\u0431\u0435\u0449\u0430\u043D\u043D\u043E\u0435."
                    : answer.trim();
                current.sendMessage(new StringTextComponent(
                    "\u00A78[\u0422\u0435\u043D\u044C]\u00A7f " + text), id);
            }));
    }

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
                ShadowEntity.class,
                player.getBoundingBox().grow(20.0D),
                s -> s.isDefeated() && player.getUniqueID().equals(s.getDefeatedOwner()))) {
            return true;
        }
        return false;
    }
}
