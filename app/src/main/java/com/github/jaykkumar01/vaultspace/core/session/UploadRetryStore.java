package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.db.UploadRetryDao;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadRetryDatabase;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadRetryEntity;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.models.base.UploadType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class UploadRetryStore {

    private final UploadRetryDao dao;

    public UploadRetryStore(@NonNull Context context){
        this.dao = UploadRetryDatabase.get(context).dao();
    }

    /* ================= Write ================= */

    public void addRetry(@NonNull String groupId,@NonNull UploadSelection s){
        dao.insert(toEntity(groupId, s));
    }

    public void addRetryBatch(
            @NonNull String groupId,
            @NonNull List<UploadSelection> selections
    ){
        if (selections.isEmpty()) return;
        List<UploadRetryEntity> list = new ArrayList<>(selections.size());
        for (UploadSelection s : selections)
            list.add(toEntity(groupId, s));
        dao.insertAll(list);
    }

    public void removeRetry(@NonNull String groupId,@NonNull UploadSelection s){
        dao.delete(groupId, s.uri.toString(), s.getType().name());
    }

    /* ================= Read ================= */

    @NonNull
    public Map<String,List<UploadSelection>> getAllRetries(){
        List<UploadRetryEntity> rows = dao.getAll();
        Map<String,List<UploadSelection>> map = new HashMap<>();
        for (UploadRetryEntity e : rows) {
            map.computeIfAbsent(e.groupId, k -> new ArrayList<>())
               .add(fromEntity(e));
        }
        return map;
    }

    /* ================= Clear ================= */

    public void clearGroup(@NonNull String groupId){
        dao.deleteGroup(groupId);
    }

    public void clearAll(){
        dao.deleteAll();
    }

    /* ================= Mapping ================= */

    private static UploadRetryEntity toEntity(
            @NonNull String groupId,
            @NonNull UploadSelection s
    ){
        return new UploadRetryEntity(
                groupId,
                s.uri.toString(),
                s.mimeType,
                s.getType().name()
        );
    }

    private static UploadSelection fromEntity(@NonNull UploadRetryEntity e){
        return new UploadSelection(
                Uri.parse(e.uri),
                e.mimeType
        );
    }
}
