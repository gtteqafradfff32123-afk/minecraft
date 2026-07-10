package com.example.titanforge.liminal.chat;

import java.util.Random;

public class AmbientMessages {
    private static final String[][] STAGES = {
        {
            "Where do you think you're going?",
            "I see you...",
            "You're not alone here.",
            "I've been waiting.",
            "This is just the beginning."
        },
        {
            "You're starting to annoy me.",
            "Every time you hit me, I learn.",
            "The walls are closing in.",
            "I can feel your fear.",
            "You're running out of time."
        },
        {
            "This is your END.",
            "I will wear your skin.",
            "Your world is already gone.",
            "Nothing remains. Only me.",
            "Give up. It hurts less."
        }
    };

    public static String getMessage(int stage, Random rng) {
        int s = Math.min(stage, STAGES.length - 1);
        String[] pool = STAGES[s];
        return pool[rng.nextInt(pool.length)];
    }
}
