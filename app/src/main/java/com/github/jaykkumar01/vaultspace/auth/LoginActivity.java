package com.github.jaykkumar01.vaultspace.auth;

import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jaykkumar01.vaultspace.R;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpaceAuth";

    private static final String AUTH_ENDPOINT =
            "https://accounts.google.com/o/oauth2/v2/auth";

    private static final String TOKEN_ENDPOINT =
            "https://oauth2.googleapis.com/token";

    private static final String REDIRECT_URI =
            "http://127.0.0.1:8080";

    private static final String DRIVE_SCOPE =
            "https://www.googleapis.com/auth/drive.file";

    private ExecutorService executor;

    // 🔐 PKCE
    private String codeVerifier;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        executor = Executors.newSingleThreadExecutor();
        applyInsets();

        findViewById(R.id.btnTestDriveOAuth)
                .setOnClickListener(v -> {
                    Log.d(TAG, "▶ OAuth (PKCE) started");
                    startOAuthFlow();
                });
    }

    /* ================= ENTRY ================= */

    private void startOAuthFlow() {
        codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);

        startLoopbackServer();
        launchOAuthConsent(codeChallenge);
    }

    /* ================= STEP 1 ================= */

    private void launchOAuthConsent(String codeChallenge) {

        String authUrl =
                AUTH_ENDPOINT
                        + "?client_id=" + Uri.encode(
                        getString(R.string.google_server_desktop_client_id))
                        + "&redirect_uri=" + Uri.encode(REDIRECT_URI)
                        + "&response_type=code"
                        + "&scope=" + Uri.encode(DRIVE_SCOPE)
                        + "&code_challenge=" + Uri.encode(codeChallenge)
                        + "&code_challenge_method=S256";

        Log.d(TAG, "OAuth URL = " + authUrl);

        CustomTabsIntent intent =
                new CustomTabsIntent.Builder().build();

        intent.launchUrl(this, Uri.parse(authUrl));
    }

    /* ================= STEP 1B ================= */

    private void startLoopbackServer() {

        executor.execute(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8080)) {

                Log.d(TAG, "✔ Loopback server started");

                Socket socket = serverSocket.accept();
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        socket.getInputStream()
                                )
                        );

                String requestLine = reader.readLine();
                Log.d(TAG, "HTTP request = " + requestLine);

                URI uri =
                        new URI("http://localhost" +
                                requestLine.split(" ")[1]);

                String code = getQueryParam(uri, "code");

                respondAndCloseTab(socket);
                socket.close();

                if (code != null) {
                    Log.d(TAG, "✔ Auth code received");
                    exchangeCodeForTokens(code);
                }

            } catch (Exception e) {
                Log.e(TAG, "Loopback error", e);
            }
        });
    }

    /* ================= STEP 2 (PKCE — FINAL) ================= */

    private void exchangeCodeForTokens(String authCode) {

        executor.execute(() -> {
            try {
                Log.d(TAG, "▶ STEP-2: Token exchange started");

                URL url = new URL(TOKEN_ENDPOINT);
                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty(
                        "Content-Type",
                        "application/x-www-form-urlencoded"
                );

                String body =
                        "client_id=" + URLEncoder.encode(
                                getString(R.string.google_server_desktop_client_id),
                                StandardCharsets.UTF_8)
                                + "&grant_type=authorization_code"
                                + "&code=" + URLEncoder.encode(
                                authCode, StandardCharsets.UTF_8)
                                + "&redirect_uri=" + URLEncoder.encode(
                                REDIRECT_URI, StandardCharsets.UTF_8)
                                + "&code_verifier=" + URLEncoder.encode(
                                codeVerifier, StandardCharsets.UTF_8);

                Log.d(TAG, "POST body length = " + body.length());

                OutputStream os = conn.getOutputStream();
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
                os.close();

                int status = conn.getResponseCode();
                InputStream stream =
                        status >= 200 && status < 300
                                ? conn.getInputStream()
                                : conn.getErrorStream();

                String response = readStream(stream);

                Log.d(TAG, "✔ Token HTTP " + status);
                Log.d(TAG, "Token response = " + response);

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "PKCE OAuth SUCCESS",
                                Toast.LENGTH_LONG
                        ).show()
                );

            } catch (Exception e) {
                Log.e(TAG, "❌ Token exchange failed", e);
            }
        });
    }

    /* ================= PKCE HELPERS ================= */

    private String generateCodeVerifier() {
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        return Base64.encodeToString(
                random,
                Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING
        );
    }

    private String generateCodeChallenge(String verifier) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");
            byte[] hash =
                    digest.digest(verifier.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(
                    hash,
                    Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* ================= UTIL ================= */

    private void respondAndCloseTab(Socket socket) throws Exception {
        OutputStream out = socket.getOutputStream();
        out.write(
                ("HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/html\r\n\r\n" +
                        "<html><body><script>window.close();</script></body></html>")
                        .getBytes()
        );
        out.flush();
    }

    private String readStream(InputStream stream) throws Exception {
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb.toString();
    }

    private String getQueryParam(URI uri, String key) {
        String query = uri.getQuery();
        if (query == null) return null;

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=");
            if (parts.length == 2 && parts[0].equals(key)) {
                return parts[1];
            }
        }
        return null;
    }

    private void applyInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );
                    return insets;
                }
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
