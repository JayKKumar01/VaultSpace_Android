package com.github.jaykkumar01.vaultspace.core.auth;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Fetches Google user profile (PRIMARY account only).
 *
 * - Owns GoogleAccountCredential internally
 * - Fetches profile name
 * - Downloads latest profile photo
 * - Saves photo privately (LOSSLESS)
 *
 * Scope:
 * https://www.googleapis.com/auth/userinfo.profile
 */
public final class GoogleUserProfileFetcher {

    private static final String TAG = "VaultSpace:UserProfile";
    private static final String USERINFO_URL =
            "https://www.googleapis.com/oauth2/v2/userinfo";

    private static final String PROFILE_PHOTO_FILE = "profile_photo.webp";

    private static final Gson GSON = new Gson();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();

    private GoogleUserProfileFetcher() {}

    /* ---------------------------------------------------
     * Public API
     * --------------------------------------------------- */

    public interface Callback {
        void onResult(@Nullable String profileName);
    }

    public static void fetch(
            @NonNull Context context,
            @NonNull String email,
            @NonNull Callback callback
    ) {
        EXECUTOR.execute(() -> {
            String profileName = null;

            try {
                GoogleAccountCredential credential =
                        GoogleAccountCredential.usingOAuth2(
                                context.getApplicationContext(),
                                Collections.singleton("https://www.googleapis.com/auth/userinfo.profile")
                        );
                credential.setSelectedAccountName(email);

                String accessToken = credential.getToken();

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(USERINFO_URL).openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty(
                        "Authorization",
                        "Bearer " + accessToken
                );
                conn.setConnectTimeout(10_000);
                conn.setReadTimeout(10_000);

                if (conn.getResponseCode() == 200) {
                    InputStreamReader reader =
                            new InputStreamReader(conn.getInputStream());

                    RawUserProfile raw =
                            GSON.fromJson(reader, RawUserProfile.class);
                    reader.close();

                    profileName = raw.name;

                    String latestPhotoUrl =
                            normalizeProfilePhotoUrl(raw.picture);

                    Bitmap bitmap = downloadBitmap(latestPhotoUrl);
                    if (bitmap != null) {
                        saveProfilePhoto(context, bitmap);
                        bitmap.recycle();
                    }

                    Log.d(TAG, "Profile fetched: " + profileName);
                } else {
                    Log.w(TAG, "UserInfo failed, HTTP " + conn.getResponseCode());
                }

            } catch (Exception e) {
                Log.w(TAG, "Failed to fetch profile", e);
            }

            String finalName = profileName;
            MAIN.post(() -> callback.onResult(finalName));
        });
    }

    /* ---------------------------------------------------
     * Internals
     * --------------------------------------------------- */

    @Nullable
    private static String normalizeProfilePhotoUrl(@Nullable String url) {
        if (url == null) return null;

        int sizeIndex = url.indexOf("=s");
        if (sizeIndex != -1) {
            url = url.substring(0, sizeIndex);
        }

        return url + "=s256-c";
    }

    @Nullable
    private static Bitmap downloadBitmap(@Nullable String url) {
        if (url == null) return null;

        try {
            HttpURLConnection conn =
                    (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(10_000);
            conn.connect();

            return BitmapFactory.decodeStream(conn.getInputStream());
        } catch (Exception e) {
            Log.w(TAG, "Failed to download photo", e);
            return null;
        }
    }

    private static void saveProfilePhoto(
            @NonNull Context context,
            @NonNull Bitmap bitmap
    ) {
        File file = new File(context.getFilesDir(), PROFILE_PHOTO_FILE);

        try (FileOutputStream out = new FileOutputStream(file)) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                bitmap.compress(
                        Bitmap.CompressFormat.WEBP_LOSSLESS,
                        100,
                        out
                );
            } else {
                bitmap.compress(
                        Bitmap.CompressFormat.PNG,
                        100,
                        out
                );
            }

            Log.d(TAG, "Profile photo saved (lossless)");

        } catch (Exception e) {
            Log.w(TAG, "Failed to save profile photo", e);
        }
    }

    /* ---------------------------------------------------
     * UI helpers
     * --------------------------------------------------- */

    @Nullable
    public static Bitmap loadSavedProfilePhoto(@NonNull Context context) {
        File file = new File(context.getFilesDir(), PROFILE_PHOTO_FILE);
        if (!file.exists()) return null;

        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    public static void clearSavedProfilePhoto(@NonNull Context context) {
        File file = new File(context.getFilesDir(), PROFILE_PHOTO_FILE);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    /* ---------------------------------------------------
     * DTO
     * --------------------------------------------------- */

    private static class RawUserProfile {
        @SerializedName("name")
        String name;

        @SerializedName("picture")
        String picture;
    }
}
