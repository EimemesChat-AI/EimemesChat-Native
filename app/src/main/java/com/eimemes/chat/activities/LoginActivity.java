package com.eimemes.chat.activities;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.eimemes.chat.R;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText emailInput, passwordInput;
    private Button btnLogin, btnSignup;
    private TextView tvToggle, tvTitle;
    private ProgressBar progressBar;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();

        tvTitle      = findViewById(R.id.tvTitle);
        emailInput   = findViewById(R.id.emailInput);
        passwordInput= findViewById(R.id.passwordInput);
        btnLogin     = findViewById(R.id.btnLogin);
        btnSignup    = findViewById(R.id.btnSignup);
        tvToggle     = findViewById(R.id.tvToggle);
        progressBar  = findViewById(R.id.progressBar);

        // Gradient title text
        applyGradientToTitle();

        btnLogin.setOnClickListener(v -> handleEmailAuth(true));
        btnSignup.setOnClickListener(v -> handleEmailAuth(false));

        tvToggle.setOnClickListener(v -> {
            isLoginMode = !isLoginMode;
            btnLogin.setVisibility(isLoginMode ? View.VISIBLE : View.GONE);
            btnSignup.setVisibility(isLoginMode ? View.GONE : View.VISIBLE);
            tvToggle.setText(isLoginMode
                ? "Don't have an account? Sign up"
                : "Already have an account? Sign in");
        });
    }

    private void applyGradientToTitle() {
        tvTitle.post(() -> {
            float width = tvTitle.getPaint().measureText(tvTitle.getText().toString());
            LinearGradient gradient = new LinearGradient(0, 0, width, 0,
                new int[]{0xFF5e9cff, 0xFFc96eff},
                null, Shader.TileMode.CLAMP);
            tvTitle.getPaint().setShader(gradient);
            tvTitle.invalidate();
        });
    }

    private void handleEmailAuth(boolean login) {
        String email    = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
            return;
        }
        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        if (login) {
            mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> goToMain())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
        } else {
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(r -> goToMain())
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                });
        }
    }

    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnSignup.setEnabled(!loading);
    }
}
