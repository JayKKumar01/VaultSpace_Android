package com.github.jaykkumar01.vaultspace.auth;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
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


public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpaceLogin";

    private CredentialManager credentialManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

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

        credentialManager = CredentialManager.create(this);

        findViewById(R.id.btnGoogleLogin)
                .setOnClickListener(v -> startGoogleLogin());
    }

    private void startGoogleLogin() {

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setServerClientId(
                                getString(R.string.google_server_client_id)
                        )
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(false) // ✅ IMPORTANT FIX
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
                                    "Google sign-in failed. Check Logcat.",
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

                // ✅ LOGIN SUCCESS
                UserSession.getInstance().setLoggedIn(true);

                Log.d(TAG, "Logged in as: " + email);
                Log.d(TAG, "ID Token length: " + idToken.length());

                Toast.makeText(
                        this,
                        "Welcome " + email,
                        Toast.LENGTH_SHORT
                ).show();

                startActivity(new Intent(this, DashboardActivity.class));
                finish();

                return;
            }
        }

        // ❌ Anything else
        Toast.makeText(
                this,
                "Unsupported credential type",
                Toast.LENGTH_SHORT
        ).show();

        Log.e(TAG, "Unexpected credential: " + credential.getClass().getName());
    }

}
