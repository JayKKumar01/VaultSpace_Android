package com.github.jaykkumar01.vaultspace.auth;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.dashboard.DashboardActivity;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import androidx.credentials.CustomCredential;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpaceLogin";
    private CredentialManager credentialManager;

    private EditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private Button btnGoogleLogin, btnEmailLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Check if already logged in
        UserSession session = UserSession.getInstance(getApplicationContext());
        if (session.isLoggedIn()) {
            startActivity(new Intent(this, DashboardActivity.class));
            finish();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );
                    return insets;
                }
        );

        initializeViews();
        credentialManager = CredentialManager.create(this);

        setupClickListeners();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);
        btnEmailLogin = findViewById(R.id.btnEmailLogin);
        btnRegister = findViewById(R.id.btnRegister);
    }

    private void setupClickListeners() {
        btnGoogleLogin.setOnClickListener(v -> startGoogleLogin());
        btnEmailLogin.setOnClickListener(v -> attemptEmailLogin());
        btnRegister.setOnClickListener(v -> showRegistrationScreen());
    }

    private void startGoogleLogin() {
        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setServerClientId(
                                getString(R.string.google_server_client_id)
                        )
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(false)
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            credentialManager.getCredentialAsync(
                    this,
                    request,
                    null,
                    getMainExecutor(),
                    new CredentialManagerCallback<
                            GetCredentialResponse,
                            GetCredentialException>() {

                        @Override
                        public void onResult(GetCredentialResponse response) {
                            handleCredential(response.getCredential());
                        }

                        @Override
                        public void onError(GetCredentialException e) {
                            Log.e(TAG, "Google login failed: " + e.getMessage(), e);
                            Toast.makeText(
                                    LoginActivity.this,
                                    "Google sign-in failed. Please try again.",
                                    Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
            );
        }
    }

    private void handleCredential(Credential credential) {
        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {

                GoogleIdTokenCredential googleCred =
                        GoogleIdTokenCredential.createFrom(customCredential.getData());

                String email = googleCred.getId();
                String idToken = googleCred.getIdToken();

                // Store the token persistently
                UserSession session = UserSession.getInstance(getApplicationContext());
                session.setGoogleLogin(idToken, email);

                Log.d(TAG, "Logged in as: " + email);
                Log.d(TAG, "ID Token: " + idToken.substring(0, Math.min(20, idToken.length())) + "...");

                // TODO: Send token to your backend for verification
                // Call your backend API to verify token and get a persistent session token
                verifyGoogleTokenWithBackend(idToken, email);

                return;
            }
        }

        Toast.makeText(
                this,
                "Unsupported credential type",
                Toast.LENGTH_SHORT
        ).show();
    }

    private void attemptEmailLogin() {
        // Reset errors
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate input
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Enter a valid email address");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            return;
        }

        if (password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        }

        // TODO: Call your backend API for email/password authentication
        authenticateWithEmailPassword(email, password);
    }

    private void authenticateWithEmailPassword(String email, String password) {
        // Show loading indicator
        showLoading(true);

        // TODO: Replace with your actual API call
        // Example Retrofit/Volley implementation:
        /*
        AuthApiService authService = RetrofitClient.getInstance().create(AuthApiService.class);
        LoginRequest loginRequest = new LoginRequest(email, password);

        authService.login(loginRequest).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    // Store tokens persistently
                    UserSession session = UserSession.getInstance(getApplicationContext());
                    session.setEmailPasswordLogin(
                        loginResponse.getAccessToken(),
                        email,
                        loginResponse.getUserId(),
                        loginResponse.getRefreshToken()
                    );

                    proceedToDashboard();
                } else {
                    Toast.makeText(LoginActivity.this,
                        "Login failed: " + response.message(),
                        Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showLoading(false);
                Toast.makeText(LoginActivity.this,
                    "Network error: " + t.getMessage(),
                    Toast.LENGTH_SHORT).show();
            }
        });
        */

        // For demo purposes - remove this when implementing real API
        showLoading(false);

        // Simulate API call
        new android.os.Handler().postDelayed(() -> {
            // This is dummy data - replace with real API response
            String dummyToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9." +
                    "eyJzdWIiOiIxMjM0NTY3ODkwIiwidXNlcklkIjoiMTIzIiwiZW1haWwiOiJ1c2VyQGV4YW1wbGUuY29tIiwiaWF0IjoxNTE2MjM5MDIyfQ." +
                    "dummy_signature_for_demo";
            String dummyUserId = "user_" + email.hashCode();
            String dummyRefreshToken = "refresh_" + System.currentTimeMillis();

            UserSession session = UserSession.getInstance(getApplicationContext());
            session.setEmailPasswordLogin(dummyToken, email, dummyUserId, dummyRefreshToken);

            proceedToDashboard();
        }, 1500);
    }

    private void verifyGoogleTokenWithBackend(String idToken, String email) {
        // TODO: Send Google ID token to your backend for verification
        // Your backend should verify the token with Google and return your own JWT token

        // Example:
        /*
        AuthApiService authService = RetrofitClient.getInstance().create(AuthApiService.class);
        GoogleLoginRequest request = new GoogleLoginRequest(idToken);

        authService.googleLogin(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    // Store your custom token from backend
                    UserSession session = UserSession.getInstance(getApplicationContext());
                    session.setEmailPasswordLogin(
                        loginResponse.getAccessToken(),
                        email,
                        loginResponse.getUserId(),
                        loginResponse.getRefreshToken()
                    );

                    proceedToDashboard();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                Toast.makeText(LoginActivity.this,
                    "Backend verification failed",
                    Toast.LENGTH_SHORT).show();
            }
        });
        */

        // For demo - proceed with Google token directly
        proceedToDashboard();
    }

    private void proceedToDashboard() {
        Toast.makeText(
                this,
                "Welcome " + UserSession.getInstance().getUserEmail(),
                Toast.LENGTH_SHORT
        ).show();

        startActivity(new Intent(this, DashboardActivity.class));
        finish();
    }

    private void showRegistrationScreen() {
        // TODO: Navigate to registration activity
        Toast.makeText(this, "Registration screen coming soon", Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean isLoading) {
        btnEmailLogin.setEnabled(!isLoading);
        btnGoogleLogin.setEnabled(!isLoading);
        etEmail.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);

        // You can add a progress bar here
        if (isLoading) {
            btnEmailLogin.setText("Signing in...");
        } else {
            btnEmailLogin.setText("Sign in with Email");
        }
    }
}