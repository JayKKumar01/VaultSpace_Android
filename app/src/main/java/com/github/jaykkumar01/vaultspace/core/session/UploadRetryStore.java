package com.github.jaykkumar01.vaultspace.core.session;

import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
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
 * Persistent store for retryable UploadSelections.
 * Session-independent. Healed on restore.
 */
public final class UploadRetryStore {

    private static final String KEY_RETRY="upload_retry_map_v2";

    private static final Type MAP_TYPE=
            new TypeToken<Map<String,List<UploadSelection>>>(){}.getType();

    private final SharedPreferences prefs;
    private final Gson gson;

    private Map<String,List<UploadSelection>> map;
    private boolean dirty;

    UploadRetryStore(@NonNull SharedPreferences prefs){
        this.prefs=prefs;
        this.gson=new GsonBuilder()
                .registerTypeAdapter(
                        Uri.class,
                        (com.google.gson.JsonSerializer<Uri>)
                                (src,t,ctx)->ctx.serialize(src.toString()))
                .registerTypeAdapter(
                        Uri.class,
                        (com.google.gson.JsonDeserializer<Uri>)
                                (json,t,ctx)->Uri.parse(json.getAsString()))
                .create();
    }

    /* ================= Load ================= */

    private Map<String,List<UploadSelection>> ensureLoaded(){
        if(map!=null) return map;
        String json=prefs.getString(KEY_RETRY,null);
        if(json==null) map=new HashMap<>();
        else{
            Map<String,List<UploadSelection>> parsed=gson.fromJson(json,MAP_TYPE);
            map=parsed!=null?parsed:new HashMap<>();
        }
        return map;
    }

    /* ================= Write ================= */

    public void addRetry(@NonNull String groupId,@NonNull UploadSelection selection){
        List<UploadSelection> list =
                ensureLoaded().computeIfAbsent(groupId,k->new ArrayList<>());
        if (!list.contains(selection)) {
            list.add(selection);
            dirty = true;
        }
    }

    public void addRetryBatch(@NonNull String groupId,@NonNull List<? extends UploadSelection> selections){
        if(selections.isEmpty()) return;

        List<UploadSelection> list =
                ensureLoaded().computeIfAbsent(groupId,k->new ArrayList<>());

        boolean changed = false;
        for (UploadSelection s : selections) {
            if (!list.contains(s)) {
                list.add(s);
                changed = true;
            }
        }

        if (changed) dirty = true;
    }


    public void removeRetry(@NonNull String groupId,@NonNull UploadSelection selection){
        List<UploadSelection> list = ensureLoaded().get(groupId);
        if (list == null) return;
        list.remove(selection);
        if (list.isEmpty()) ensureLoaded().remove(groupId);
        dirty = true;
    }


    /* ================= Read ================= */

    @NonNull
    public Map<String,List<UploadSelection>> getAllRetries(){
        Map<String,List<UploadSelection>> src = ensureLoaded();
        Map<String,List<UploadSelection>> copy = new HashMap<>(src.size());
        for (Map.Entry<String,List<UploadSelection>> e : src.entrySet()) {
            copy.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
        return copy;
    }


    /* ================= Clear ================= */

    public void clearGroup(@NonNull String groupId){
        ensureLoaded().remove(groupId);
        dirty=true;
    }

    public void clearAll(){
        map=null;
        dirty=false;
        prefs.edit().remove(KEY_RETRY).apply();
    }

    /* ================= Persist ================= */

    public void flush(){
        if(!dirty||map==null) return;
        prefs.edit().putString(KEY_RETRY,gson.toJson(map)).apply();
        dirty=false;
    }

    /* ================= Atomic Helpers ================= */

    public void addRetryBatchAndFlush(
            @NonNull String groupId,
            @NonNull List<? extends UploadSelection> selections
    ){
        addRetryBatch(groupId, selections);
        flush();
    }

    public void removeRetryAndFlush(
            @NonNull String groupId,
            @NonNull UploadSelection selection
    ){
        removeRetry(groupId, selection);
        flush();
    }

    public void clearGroupAndFlush(@NonNull String groupId){
        clearGroup(groupId);
        flush();
    }

}
