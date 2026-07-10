package com.example.titanforge.liminal.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class GroqChatAPI {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String API_KEY = "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";

    public static String ask(String playerName, String message) {
        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            JsonObject body = new JsonObject();
            body.addProperty("model", "mixtral-8x7b-32768");

            JsonArray messages = new JsonArray();

            JsonObject system = new JsonObject();
            system.addProperty("role", "system");
            system.addProperty("content",
                "You are the evil shadow twin of the player " + playerName
                + " trapped in the Liminal dimension. Mock, taunt, and threaten them. "
                + "Keep responses under 200 characters. Be creepy, personal, and speak in first person "
                + "as their dark reflection. Use short sentences. Never break character.");
            messages.add(system);

            JsonObject user = new JsonObject();
            user.addProperty("role", "user");
            user.addProperty("content", message);
            messages.add(user);

            body.add("messages", messages);
            body.addProperty("max_tokens", 100);
            body.addProperty("temperature", 0.8);

            byte[] data = new Gson().toJson(body).getBytes(StandardCharsets.UTF_8);
            conn.setFixedLengthStreamingMode(data.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(data);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                InputStream is = conn.getInputStream();
                Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
                String json = s.hasNext() ? s.next() : "{}";
                s.close();

                JsonObject resp = new Gson().fromJson(json, JsonObject.class);
                return resp.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
