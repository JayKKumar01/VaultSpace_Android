package com.github.jaykkumar01.vaultspace.core.session;

import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.models.MediaSelection;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UploadRetryStore
 *
 * Optimized for bulk mutation:
 * - load once
 * - add many
 * - save once
 */
public final class UploadRetryStore {

    private static final String KEY_RETRY = "upload_retry_map_v1";

    private static final Type MAP_TYPE =
            new TypeToken<Map<String, List<MediaSelection>>>() {}.getType();

    private final SharedPreferences prefs;
    private final Gson gson;

    private Map<String, List<MediaSelection>> map;
    private boolean dirty;

    UploadRetryStore(SharedPreferences prefs) {
        this.prefs = prefs;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class,
                        (com.google.gson.JsonSerializer<Uri>)
                                (src, t, ctx) -> ctx.serialize(src.toString()))
                .registerTypeAdapter(Uri.class,
                        (com.google.gson.JsonDeserializer<Uri>)
                                (json, t, ctx) -> Uri.parse(json.getAsString()))
                .create();
    }

    /* ================= LOAD ================= */

    private Map<String, List<MediaSelection>> ensureLoaded() {
        if (map != null) return map;

        String json = prefs.getString(KEY_RETRY, null);
        if (json == null) {
            map = new HashMap<>();
        } else {
            Map<String, List<MediaSelection>> parsed =
                    gson.fromJson(json, MAP_TYPE);
            map = parsed != null ? parsed : new HashMap<>();
        }

        return map;
    }

    /* ================= WRITE ================= */

    public void addRetry(
            @NonNull String albumId,
            @NonNull MediaSelection selection
    ) {
        List<MediaSelection> list =
                ensureLoaded().computeIfAbsent(albumId, k -> new ArrayList<>());

        list.add(selection);
        dirty = true;
    }

    /** 🔥 BULK ADD — this is what UploadManager really needs */
    public void addRetryBatch(
            @NonNull String albumId,
            @NonNull List<MediaSelection> selections
    ) {
        if (selections.isEmpty()) return;

        List<MediaSelection> list =
                ensureLoaded().computeIfAbsent(albumId, k -> new ArrayList<>());

        list.addAll(selections);
        dirty = true;
    }

    /* ================= READ ================= */

    /** Bulk read — you already said this is how you’ll consume it */
    @NonNull
    public Map<String, List<MediaSelection>> getAllRetries() {
        return ensureLoaded();
    }

    /* ================= CLEAR ================= */

    public void clearAlbum(@NonNull String albumId) {
        ensureLoaded().remove(albumId);
        dirty = true;
    }

    public void clearAll() {
        map = null;
        dirty = false;
        prefs.edit().remove(KEY_RETRY).apply();
    }

    /* ================= PERSIST ================= */

    /** Call ONCE after loops */
    public void flush() {
        if (!dirty || map == null) return;

        prefs.edit()
                .putString(KEY_RETRY, gson.toJson(map))
                .apply();

        dirty = false;
    }
}
