package com.eimemes.chat.network;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import java.util.concurrent.TimeUnit;

public class StreamingClient {

    // Your Vercel backend URL
    private static final String API_URL = "https://eimemeschat-ai-ashy.vercel.app/api/chat";

    public interface StreamCallback {
        void onToken(String token);       // called for each word/token
        void onDone(String fullText, String model, boolean disclaimer);
        void onError(String error);
    }

    private final OkHttpClient client;
    private Call activeCall;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public StreamingClient() {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();
    }

    public void sendMessage(String message, JSONArray history, String idToken, StreamCallback cb) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("message", message);
                body.put("history", history != null ? history : new JSONArray());

                RequestBody reqBody = RequestBody.create(
                    body.toString(),
                    MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                    .url(API_URL)
                    .post(reqBody)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + idToken)
                    .build();

                activeCall = client.newCall(request);
                Response response = activeCall.execute();

                if (!response.isSuccessful()) {
                    mainHandler.post(() -> cb.onError("Server error: " + response.code()));
                    return;
                }

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(response.body().byteStream())
                );

                StringBuilder fullText = new StringBuilder();
                String line;
                String finalModel = "";
                boolean finalDisclaimer = false;

                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6).trim();
                        if (data.isEmpty()) continue;
                        try {
                            JSONObject parsed = new JSONObject(data);
                            if (parsed.has("token")) {
                                String token = parsed.getString("token");
                                fullText.append(token);
                                String tokenCopy = token;
                                mainHandler.post(() -> cb.onToken(tokenCopy));
                            } else if (parsed.optBoolean("done", false)) {
                                finalModel      = parsed.optString("model", "");
                                finalDisclaimer = parsed.optBoolean("disclaimer", false);
                            } else if (parsed.has("error")) {
                                String err = parsed.getString("error");
                                mainHandler.post(() -> cb.onError(err));
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                }

                String finalText       = fullText.toString();
                String finalModelCopy  = finalModel;
                boolean finalDiscCopy  = finalDisclaimer;
                mainHandler.post(() -> cb.onDone(finalText, finalModelCopy, finalDiscCopy));

            } catch (Exception e) {
                if (e.getMessage() != null && e.getMessage().contains("canceled")) return;
                mainHandler.post(() -> cb.onError("Connection error. Check your internet."));
            }
        }).start();
    }

    public void cancel() {
        if (activeCall != null) activeCall.cancel();
    }
}
