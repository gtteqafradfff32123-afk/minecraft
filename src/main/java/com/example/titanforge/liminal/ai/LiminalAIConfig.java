package com.example.titanforge.liminal.ai;

import com.example.titanforge.TitanForge;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class LiminalAIConfig {
    private static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> ENDPOINT;
    public static final ForgeConfigSpec.ConfigValue<String> MODEL;
    public static final ForgeConfigSpec.IntValue MAX_TOKENS;
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE;

    static {
        Pair<Config, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Config::new);
        SPEC = pair.getRight();
        Config c = pair.getLeft();
        API_KEY = c.apiKey;
        ENDPOINT = c.endpoint;
        MODEL = c.model;
        MAX_TOKENS = c.maxTokens;
        TEMPERATURE = c.temperature;
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "titanforge_liminal.toml");
    }

    private static class Config {
        public final ForgeConfigSpec.ConfigValue<String> apiKey;
        public final ForgeConfigSpec.ConfigValue<String> endpoint;
        public final ForgeConfigSpec.ConfigValue<String> model;
        public final ForgeConfigSpec.IntValue maxTokens;
        public final ForgeConfigSpec.DoubleValue temperature;

        public Config(ForgeConfigSpec.Builder b) {
            b.push("ai");
            apiKey = b
                .comment("API key for Groq/xAI. Get one at https://console.groq.com or https://x.ai")
                .define("api_key", "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");
            endpoint = b
                .comment("API endpoint URL. Groq: https://api.groq.com/openai/v1/chat/completions, xAI: https://api.x.ai/v1/chat/completions")
                .define("endpoint", "https://api.groq.com/openai/v1");
            model = b
                .comment("Model name. Groq: llama-3.3-70b-versatile, xAI: grok-2-latest")
                .define("model", "llama-3.3-70b-versatile");
            maxTokens = b
                .comment("Max tokens per response")
                .defineInRange("max_tokens", 150, 30, 500);
            temperature = b
                .comment("LLM temperature (0.0-1.0)")
                .defineInRange("temperature", 0.9, 0.0, 1.0);
            b.pop();
        }
    }
}
