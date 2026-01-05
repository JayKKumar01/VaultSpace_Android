package com.github.jaykkumar01.vaultspace.auth;

import android.net.Uri;
import android.os.Bundle;
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
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "VaultSpaceAuth";

    // 🔐 MUST match Google Console
    private static final String REDIRECT_URI = "http://127.0.0.1:8080";
    private static final String DRIVE_SCOPE =
            "https://www.googleapis.com/auth/drive.file";

    private ExecutorService serverExecutor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        serverExecutor = Executors.newSingleThreadExecutor();

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {
                    Insets bars =
                            insets.getInsets(WindowInsetsCompat.Type.systemBars());
                    v.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );
                    return insets;
                }
        );

        findViewById(R.id.btnSelectPrimaryDrive)
                .setOnClickListener(v ->
                        Toast.makeText(
                                this,
                                "Primary flow later",
                                Toast.LENGTH_SHORT
                        ).show()
                );

        findViewById(R.id.btnTestDriveOAuth)
                .setOnClickListener(v -> {
                    Log.d(TAG, "Starting Loopback OAuth");
                    startLocalServer();
                    launchOAuthConsent();
                });
    }

    /* ---------------------------------------------------
     * STEP 1A: Launch OAuth consent
     * --------------------------------------------------- */
    private void launchOAuthConsent() {

        String url =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id=" + Uri.encode(
                        getString(R.string.google_server_web_client_id))
                        + "&redirect_uri=" + Uri.encode(REDIRECT_URI)
                        + "&response_type=code"
                        + "&scope=" + Uri.encode(DRIVE_SCOPE)
                        + "&access_type=offline"
                        + "&prompt=consent";

        Log.d(TAG, "OAuth URL:\n" + url);

        CustomTabsIntent intent =
                new CustomTabsIntent.Builder().build();

        intent.launchUrl(this, Uri.parse(url));
    }

    /* ---------------------------------------------------
     * STEP 1B: Local loopback server
     * --------------------------------------------------- */
    private void startLocalServer() {

        serverExecutor.execute(() -> {
            try (ServerSocket serverSocket = new ServerSocket(8080)) {

                Log.d(TAG, "Local server started on 127.0.0.1:8080");

                Socket socket = serverSocket.accept();
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(socket.getInputStream())
                        );

                String requestLine = reader.readLine();
                Log.d(TAG, "HTTP request: " + requestLine);

                if (requestLine != null && requestLine.startsWith("GET")) {

                    String path =
                            requestLine.split(" ")[1];

                    URI uri =
                            new URI("http://localhost" + path);

                    String code =
                            getQueryParam(uri, "code");
                    String error =
                            getQueryParam(uri, "error");

                    OutputStream output =
                            socket.getOutputStream();

                    String response;

                    if (code != null) {
                        Log.d(TAG, "Authorization code received");
                        Log.d(TAG, "Code length: " + code.length());

                        response =
                                "<html><body><h3>You may close this window.</h3></body></html>";

                        runOnUiThread(() ->
                                Toast.makeText(
                                        this,
                                        "Drive access approved (code received)",
                                        Toast.LENGTH_SHORT
                                ).show()
                        );

                        // 🔜 NEXT STEP:
                        // Exchange this `code` for access + refresh token
                    } else {
                        Log.e(TAG, "OAuth error: " + error);
                        response =
                                "<html><body><h3>Authorization failed.</h3></body></html>";
                    }

                    output.write(
                            ("HTTP/1.1 200 OK\r\n"
                                    + "Content-Type: text/html\r\n\r\n"
                                    + response).getBytes()
                    );
                    output.flush();
                }

                socket.close();
                Log.d(TAG, "Local server closed");

            } catch (Exception e) {
                Log.e(TAG, "Loopback server error", e);
            }
        });
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serverExecutor.shutdownNow();
    }
}
