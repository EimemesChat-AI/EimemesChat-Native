package com.eimemes.chat;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}
Then update AndroidManifest.xml — add android:name=".App" to the application tag:
<application
    android:name=".App"
    android:allowBackup="true"
    android:label="@string/app_name"
    android:theme="@style/Theme.EimemesChat"
    android:hardwareAccelerated="true"
    android:usesCleartextTraffic="true">
