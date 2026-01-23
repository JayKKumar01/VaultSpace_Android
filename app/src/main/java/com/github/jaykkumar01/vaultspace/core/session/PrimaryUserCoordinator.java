package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PrimaryUserCoordinator {

    private static final String TAG = "VaultSpace:PrimaryUser";
    private static final String USERINFO_URL =
            "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String PHOTO_FILE = "profile_photo.webp";

    private static final ExecutorService EXECUTOR =
            Executors.newSingleThreadExecutor();
    private static final Handler MAIN =
            new Handler(Looper.getMainLooper());
    private static final Gson GSON = new Gson();

    private PrimaryUserCoordinator() {}



    /* ==========================================================
     * Listener
     * ========================================================== */

    public interface Callback {
        void onSuccess();
        void onError();
    }

    /* ==========================================================
     * LOGIN — CREATE PRIMARY USER (ATOMIC)
     * ========================================================== */

    public static void prepare(
            @NonNull Context context,
            @NonNull String email,
            @NonNull Callback callback
    ) {
        Context app = context.getApplicationContext();

        EXECUTOR.execute(() -> {
            try {
                ProfileData data = fetchProfile(app, email);

                if (data.name == null || data.photo == null) {
                    throw new RuntimeException("Incomplete profile data");
                }

                // write photo first
                writeBitmap(app, data.photo);
                data.photo.recycle();

                // commit session only after everything succeeded
                UserSession session = new UserSession(app);
                session.savePrimaryAccountEmail(email);
                session.saveProfileName(data.name);

                MAIN.post(callback::onSuccess);

            } catch (Exception e) {
                Log.w(TAG, "prepare() failed", e);
                MAIN.post(callback::onError);
            }
        });
    }

    /* ==========================================================
     * DASHBOARD — REFRESH PROFILE (FAIL-SOFT)
     * ========================================================== */

    public static void refresh(
            @NonNull Context context,
            @NonNull Callback callback
    ) {
        Context app = context.getApplicationContext();

        EXECUTOR.execute(() -> {
            try {
                UserSession session = new UserSession(app);
                String email = session.getPrimaryAccountEmail();
                if (email == null) throw new RuntimeException("Missing email");

                ProfileData data = fetchProfile(app, email);

                if (data.name != null) {
                    session.saveProfileName(data.name);
                }

                if (data.photo != null) {
                    writeBitmap(app, data.photo);
                    data.photo.recycle();
                }

                MAIN.post(callback::onSuccess);

            } catch (Exception e) {
                Log.w(TAG, "refresh() failed", e);
                MAIN.post(callback::onError);
            }
        });
    }

    /* ==========================================================
     * PUBLIC UTILITY
     * ========================================================== */

    @Nullable
    public static Bitmap loadProfilePhoto(@NonNull Context context) {
        File file = new File(
                context.getApplicationContext().getFilesDir(),
                PHOTO_FILE
        );
        return file.exists()
                ? BitmapFactory.decodeFile(file.getAbsolutePath())
                : null;
    }

    public static void clearSavedProfilePhoto(@NonNull Context context) {
        File file = new File(
                context.getApplicationContext().getFilesDir(),
                PHOTO_FILE
        );
        if (file.exists()){
            boolean ignored = file.delete();
        }
    }

    /* ==========================================================
     * INTERNAL FETCH
     * ========================================================== */

    private static ProfileData fetchProfile(
            @NonNull Context context,
            @NonNull String email
    ) throws Exception {

        String token = GoogleCredentialFactory.forProfile(context, email).getToken();

        InputStreamReader reader = getStreamReader(token);

        RawProfile raw =
                GSON.fromJson(reader, RawProfile.class);
        reader.close();

        Bitmap photo = null;
        if (raw.picture != null) {
            photo = downloadBitmap(normalizePhotoUrl(raw.picture));
        }

        return new ProfileData(raw.name, photo);
    }

    @NonNull
    private static InputStreamReader getStreamReader(String token) throws IOException {
        HttpURLConnection conn =
                (HttpURLConnection) new URL(USERINFO_URL).openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty(
                "Authorization",
                "Bearer " + token
        );
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        if (conn.getResponseCode() != 200) {
            throw new RuntimeException(
                    "UserInfo HTTP " + conn.getResponseCode()
            );
        }

        return new InputStreamReader(conn.getInputStream());
    }

    /* ==========================================================
     * FILE / NET HELPERS
     * ========================================================== */

    private static void writeBitmap(
            @NonNull Context context,
            @NonNull Bitmap bitmap
    ) throws Exception {
        File file = new File(context.getFilesDir(), PHOTO_FILE);
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
        }
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
            Log.w(TAG, "Photo download failed", e);
            return null;
        }
    }

    @Nullable
    private static String normalizePhotoUrl(@Nullable String url) {
        if (url == null) return null;
        int idx = url.indexOf("=s");
        if (idx != -1) url = url.substring(0, idx);
        return url + "=s256-c";
    }

    /* ==========================================================
     * DTOs
     * ========================================================== */

    private static class ProfileData {
        final String name;
        final Bitmap photo;

        ProfileData(String name, Bitmap photo) {
            this.name = name;
            this.photo = photo;
        }
    }

    private static class RawProfile {
        @SerializedName("name")
        String name;
        @SerializedName("picture")
        String picture;
    }
}
