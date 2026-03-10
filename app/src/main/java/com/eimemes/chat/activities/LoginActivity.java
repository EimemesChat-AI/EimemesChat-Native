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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;

    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private EditText emailInput, passwordInput;
    private Button btnLogin, btnSignup, btnGoogle;
    private TextView tvToggle, tvTitle;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth         = FirebaseAuth.getInstance();
        tvTitle       = findViewById(R.id.tvTitle);
        emailInput    = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        btnLogin      = findViewById(R.id.btnLogin);
        btnSignup     = findViewById(R.id.btnSignup);
        btnGoogle     = findViewById(R.id.btnGoogle);
        tvToggle      = findViewById(R.id.tvToggle);
        progressBar   = findViewById(R.id.progressBar);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("230417181657-7v30t8ogq03broga9p676p3f9lltng1a.apps.googleusercontent.com")
            .requestEmail()
            .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        applyGradient();

        btnLogin.setOnClickListener(v -> handleAuth(true));
        btnSignup.setOnClickListener(v -> handleAuth(false));
        btnGoogle.setOnClickListener(v -> signInWithGoogle());

        tvToggle.setOnClickListener(v -> {
            boolean showLogin = btnLogin.getVisibility() == View.GONE;
            btnLogin.setVisibility(showLogin ? View.VISIBLE : View.GONE);
            btnSignup.setVisibility(showLogin ? View.GONE : View.VISIBLE);
            tvToggle.setText(showLogin
                ? "Don't have an account? Sign up"
                : "Already have an account? Sign in");
        });
    }

    private void signInWithGoogle() {
        setLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                setLoading(false);
                Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
            .addOnSuccessListener(r -> goToMain())
            .addOnFailureListener(e -> {
                setLoading(false);
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private void applyGradient() {
        tvTitle.post(() -> {
            float w = tvTitle.getPaint().measureText(tvTitle.getText().toString());
            LinearGradient g = new LinearGradient(0, 0, w, 0,
                new int[]{0xFF5e9cff, 0xFFc96eff}, null, Shader.TileMode.CLAMP);
            tvTitle.getPaint().setShader(g);
            tvTitle.invalidate();
        });
    }

    private void handleAuth(boolean login) {
        String email    = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Enter email and password", Toast.LENGTH_SHORT).show();
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

    private void setLoading(boolean on) {
        progressBar.setVisibility(on ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!on);
        btnSignup.setEnabled(!on);
        btnGoogle.setEnabled(!on);
    }
}
