package com.example.titanforge.liminal.ai;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GrokClient {
    private static final Gson GSON = new Gson();
    private static final String HARDCODED_API_KEY = "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";
    private static final String HARDCODED_ENDPOINT = "https://api.groq.com/openai/v1";
    private static final String HARDCODED_MODEL = "llama-3.3-70b-versatile";
    private static final int HARDCODED_MAX_TOKENS = 65;
    private static final double HARDCODED_TEMPERATURE = 0.5;

    static {
        com.example.titanforge.TitanForge.LOGGER.info("[GrokClient] initialized with key={}... and endpoint={}",
            HARDCODED_API_KEY.substring(0, Math.min(8, HARDCODED_API_KEY.length())), HARDCODED_ENDPOINT);
    }

    public static CompletableFuture<String> ask(String systemPrompt, List<Message> history, String userMsg) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String json = buildBody(systemPrompt, history, userMsg);
                String key = HARDCODED_API_KEY;
                URL url = new URL(HARDCODED_ENDPOINT + "/chat/completions");
                HttpURLConnection c = (HttpURLConnection) url.openConnection();
                c.setRequestMethod("POST");
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("Accept", "application/json");
                c.setRequestProperty("User-Agent", "TitanForge/1.0");
                c.setRequestProperty("Authorization", "Bearer " + key);
                c.setDoOutput(true);
                c.setConnectTimeout(10000);
                c.setReadTimeout(20000);

                byte[] data = json.getBytes(StandardCharsets.UTF_8);
                c.setFixedLengthStreamingMode(data.length);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(data);
                }

                int code = c.getResponseCode();
                if (code != 200) {
                    BufferedReader err = new BufferedReader(new InputStreamReader(c.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder eb = new StringBuilder();
                    String line;
                    while ((line = err.readLine()) != null) eb.append(line);
                    err.close();
                    com.example.titanforge.TitanForge.LOGGER.error("[GrokClient] API error {}: {} | key={} endpoint={}", code, eb.toString(), key.substring(0, Math.min(8, key.length())) + "...", HARDCODED_ENDPOINT);
                    return null;
                }
                com.example.titanforge.TitanForge.LOGGER.info("[GrokClient] API call successful (code 200) — AI chat is working");

                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder sb = new StringBuilder();
                String l;
                while ((l = br.readLine()) != null) sb.append(l);
                br.close();
                return parseReply(sb.toString());
            } catch (Exception e) {
                com.example.titanforge.TitanForge.LOGGER.error("[GrokClient] request failed: {}", e.getMessage());
                return null;
            }
        });
    }

    private static String buildBody(String systemPrompt, List<Message> history, String userMsg) {
        JsonObject body = new JsonObject();
        body.addProperty("model", HARDCODED_MODEL);
        body.addProperty("temperature", HARDCODED_TEMPERATURE);
        body.addProperty("max_tokens", HARDCODED_MAX_TOKENS);

        JsonArray messages = new JsonArray();
        messages.add(msg("system", systemPrompt));
        for (Message m : history) {
            messages.add(msg(m.role, m.content));
        }
        messages.add(msg("user", userMsg));
        body.add("messages", messages);
        return GSON.toJson(body);
    }

    private static String parseReply(String json) {
        try {
            JsonObject resp = GSON.fromJson(json, JsonObject.class);
            String text = resp.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString().trim();
            return text;
        } catch (Exception e) {
            return null;
        }
    }

    private static JsonObject msg(String role, String content) {
        JsonObject o = new JsonObject();
        o.addProperty("role", role);
        o.addProperty("content", content);
        return o;
    }

    public static class Message {
        public final String role;
        public final String content;
        public Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
