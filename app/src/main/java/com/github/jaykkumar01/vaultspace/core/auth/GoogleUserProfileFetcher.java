package com.github.jaykkumar01.vaultspace.core.auth;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.gson.Gson;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Static utility to fetch basic Google profile info (name + photo)
 * using existing OAuth credential.
 *
 * - Uses ExecutorService (single thread)
 * - Handles background threading internally
 * - Callback always delivered on main thread
 * - Never throws to caller
 *
 * Requires scope:
 * https://www.googleapis.com/auth/userinfo.profile
 */
public final class GoogleUserProfileFetcher {

    private static final String TAG = "VaultSpace:UserProfile";
    private static final String USERINFO_URL =
            "https://www.googleapis.com/oauth2/v2/userinfo";

    private static final Gson GSON = new Gson();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    // Single-thread executor for auth/profile work
    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private GoogleUserProfileFetcher() {}

    /* ---------------------------------------------------
     * Public API (ASYNC)
     * --------------------------------------------------- */

    public interface Callback {
        void onResult(@Nullable GoogleUserProfile profile);
    }

    public static void fetch(
            @NonNull GoogleAccountCredential credential,
            @NonNull Callback callback
    ) {
        EXECUTOR.execute(() -> {
            GoogleUserProfile result = null;

            try {
                String accessToken = credential.getToken();

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(USERINFO_URL).openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty(
                        "Authorization",
                        "Bearer " + accessToken
                );
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);

                if (conn.getResponseCode() == 200) {
                    InputStreamReader reader =
                            new InputStreamReader(conn.getInputStream());

                    result = GSON.fromJson(reader, GoogleUserProfile.class);
                    reader.close();

                    Log.d(TAG, "Profile fetched: " + result.name);
                } else {
                    Log.w(TAG, "UserInfo request failed, HTTP "
                            + conn.getResponseCode());
                }

            } catch (Exception e) {
                Log.w(TAG, "Failed to fetch user profile info", e);
            }

            GoogleUserProfile finalResult = result;
            MAIN.post(() -> callback.onResult(finalResult));
        });
    }

    /* ---------------------------------------------------
     * DTO
     * --------------------------------------------------- */

    public static class GoogleUserProfile {
        public String name;
        public String picture;
        public String email;
    }
}
