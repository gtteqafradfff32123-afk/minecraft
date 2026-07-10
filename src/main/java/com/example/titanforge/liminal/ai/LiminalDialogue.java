package com.example.titanforge.liminal.ai;

import java.util.*;

public class LiminalDialogue {
    private static final Map<UUID, Deque<GrokClient.Message>> HISTORY = new HashMap<>();
    private static final int MAX_HISTORY = 12;

    public static List<GrokClient.Message> get(UUID player) {
        return new ArrayList<>(HISTORY.getOrDefault(player, new ArrayDeque<>()));
    }

    public static void addUser(UUID player, String msg) {
        add(player, "user", msg);
    }

    public static void addCopy(UUID player, String msg) {
        add(player, "assistant", msg);
    }

    private static void add(UUID player, String role, String content) {
        Deque<GrokClient.Message> h = HISTORY.computeIfAbsent(player, k -> new ArrayDeque<>());
        h.addLast(new GrokClient.Message(role, content));
        while (h.size() > MAX_HISTORY) h.removeFirst();
    }

    public static void clear(UUID player) {
        HISTORY.remove(player);
    }
}
