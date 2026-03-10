package com.eimemes.chat;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                    .setApplicationId("1:230417181657:android:577c478074c10436f387c8")
                    .setProjectId("chat-eimeme")
                    .setApiKey("AIzaSyBBAw2643r9q4mKpFU2uUUiCpu7Fzb287w")
                    .setStorageBucket("chat-eimeme.firebasestorage.app")
                    .setGcmSenderId("230417181657")
                    .build();
                FirebaseApp.initializeApp(this, options);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
