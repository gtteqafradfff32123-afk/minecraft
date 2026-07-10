package com.example.titanforge.liminal.chat;

import com.example.titanforge.liminal.LiminalManager;
import com.example.titanforge.liminal.ai.GrokClient;
import com.example.titanforge.liminal.ai.LiminalDialogue;
import com.example.titanforge.liminal.ai.LiminalPrompt;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.StringTextComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LiminalChatAI {

    private static final String[] FALLBACKS = {
        "...",
        "\u0414\u0443\u043C\u0430\u0435\u0448\u044C, \u0441\u043C\u043E\u0436\u0435\u0448\u044C \u0441\u0431\u0435\u0436\u0430\u0442\u044C?",
        "\u042F \u2014 \u0442\u0432\u043E\u0439 \u0441\u0442\u0440\u0430\u0445 \u0432\u043E \u0442\u044C\u043C\u0435.",
        "\u041C\u044B \u043E\u0434\u043D\u043E \u0438 \u0442\u043E \u0436\u0435, \u0442\u044B \u0438 \u044F.",
        "\u041A\u0430\u0436\u0434\u044B\u0439 \u0448\u0430\u0433 \u043F\u0440\u0438\u0431\u043B\u0438\u0436\u0430\u0435\u0442 \u0442\u0435\u0431\u044F \u043A\u043E \u043C\u043D\u0435.",
        "\u0422\u044B \u043D\u0435 \u043C\u043E\u0436\u0435\u0448\u044C \u0441\u043F\u0440\u044F\u0442\u0430\u0442\u044C\u0441\u044F \u043E\u0442 \u0441\u0435\u0431\u044F.",
        "\u042F \u0437\u043D\u0430\u044E \u0432\u0441\u0451, \u0447\u0435\u0433\u043E \u0442\u044B \u0431\u043E\u0438\u0448\u044C\u0441\u044F.",
        "\u0421\u0442\u0435\u043D\u044B \u043D\u0430\u0431\u043B\u044E\u0434\u0430\u044E\u0442 \u0437\u0430 \u0442\u043E\u0431\u043E\u0439.",
        "\u041E\u0441\u0442\u0430\u043D\u044C\u0441\u044F \u043D\u0430\u0432\u0441\u0435\u0433\u0434\u0430...",
        "\u0412\u044B\u0445\u043E\u0434\u0430 \u043D\u0435\u0442. \u0422\u043E\u043B\u044C\u043A\u043E \u043C\u044B."
    };

    private static final Map<UUID, Long> lastCall = new HashMap<>();
    private static final long COOLDOWN_MS = 3000;

    public static void onPlayerMessage(ServerPlayerEntity player, String rawMsg) {
        UUID pid = player.getUniqueID();
        if (!canCall(pid)) {
            sendToPlayer(player, "...");
            return;
        }

        LiminalManager.State st = LiminalManager.getState(pid);
        if (st == null) return;

        int minutesLeft = Math.max(0, (st.durationTicks - st.ticks) / (60 * 20));
        String playerName = player.getGameProfile().getName();
        String sysPrompt = LiminalPrompt.build(
            playerName, minutesLeft, st.copiesKilled, st.activeCopies);
        String intentHint = LiminalManager.applyChatIntent(player, rawMsg);
        String aiInput = intentHint == null ? rawMsg : rawMsg + "\n[\u0421\u0418\u0421\u0422\u0415\u041C\u0410: " + intentHint + ". \u041E\u0442\u0432\u0435\u0442\u044C \u0438\u0433\u0440\u043E\u043A\u0443 \u0432 \u0440\u043E\u043B\u0438 \u0442\u0435\u043D\u0438 \u0438 \u043F\u0440\u0438\u0437\u043D\u0430\u0442\u044C \u043D\u043E\u0432\u043E\u0435 \u043F\u043E\u0432\u0435\u0434\u0435\u043D\u0438\u0435.]";

        LiminalDialogue.addUser(pid, rawMsg);

        GrokClient.ask(sysPrompt, LiminalDialogue.get(pid), aiInput)
            .thenAccept(reply -> {
                player.server.execute(() -> {
                    ServerPlayerEntity p = player.server.getPlayerList().getPlayerByUUID(pid);
                    if (p == null || !LiminalManager.isInside(p)) return;
                    String text = (reply == null || reply.isEmpty())
                        ? FALLBACKS[player.getRNG().nextInt(FALLBACKS.length)]
                        : reply;
                    LiminalDialogue.addCopy(pid, text);
                    LiminalManager.sendCopyMessage(p, text);
                });
            });
    }

    public static void copySpeaks(ServerPlayerEntity player, String situation) {
        UUID pid = player.getUniqueID();
        if (!canCall(pid)) return;

        LiminalManager.State st = LiminalManager.getState(pid);
        if (st == null) return;

        int minutesLeft = Math.max(0, (st.durationTicks - st.ticks) / (60 * 20));
        String playerName = player.getGameProfile().getName();
        String sysPrompt = LiminalPrompt.build(
            playerName, minutesLeft, st.copiesKilled, st.activeCopies);

        GrokClient.ask(sysPrompt, LiminalDialogue.get(pid), "[\u0421\u0418\u0422\u0423\u0410\u0426\u0418\u042F: " + situation + "]")
            .thenAccept(reply -> {
                player.server.execute(() -> {
                    ServerPlayerEntity p = player.server.getPlayerList().getPlayerByUUID(pid);
                    if (p == null || !LiminalManager.isInside(p)) return;
                    String text = (reply == null || reply.isEmpty())
                        ? FALLBACKS[player.getRNG().nextInt(FALLBACKS.length)]
                        : reply;
                    LiminalDialogue.addCopy(pid, text);
                    LiminalManager.sendCopyMessage(p, text);
                });
            });
    }

    public static void sendToPlayer(ServerPlayerEntity player, String text) {
        try {
            player.server.execute(() -> {
                if (player.isAlive()) {
                    String name = player.getGameProfile().getName();
                    player.sendMessage(
                        new StringTextComponent("\u00A78[\u00A77" + name + "\u00A78]\u00A7f " + text),
                        player.getUniqueID()
                    );
                }
            });
        } catch (Exception ignored) {}
    }

    private static boolean canCall(UUID player) {
        long now = System.currentTimeMillis();
        if (now - lastCall.getOrDefault(player, 0L) < COOLDOWN_MS) return false;
        lastCall.put(player, now);
        return true;
    }
}
