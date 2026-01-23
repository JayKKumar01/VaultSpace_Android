package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.db.UploadRetryDao;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadRetryDatabase;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadRetryEntity;
import com.github.jaykkumar01.vaultspace.core.upload.base.FailureReason;
import com.github.jaykkumar01.vaultspace.core.upload.drive.UploadDriveHelper;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public final class UploadRetryStore {

    private final UploadRetryDao dao;

    public UploadRetryStore(@NonNull Context context) {
        this.dao = UploadRetryDatabase.get(context).dao();
    }

    /* ================= Write ================= */

    public void addRetry(@NonNull String groupId, @NonNull UploadSelection s) {
        dao.insert(toEntity(groupId, s));
    }

    public void addRetryBatch(@NonNull String groupId, @NonNull List<UploadSelection> selections) {
        if (selections.isEmpty()) return;
        List<UploadRetryEntity> list = new ArrayList<>(selections.size());
        for (UploadSelection s : selections) list.add(toEntity(groupId, s));
        dao.insertAll(list);
    }

    public void removeRetry(@NonNull String groupId, @NonNull UploadSelection s) {
        dao.delete(groupId, s.uri.toString(), s.type.name());
    }

    public void removeRetryByUris(@NonNull String groupId, @NonNull List<Uri> uris) {
        if (uris.isEmpty()) return;
        List<String> list = new ArrayList<>(uris.size());
        for (Uri u : uris) list.add(u.toString());
        dao.deleteByUris(groupId, list);
    }

    /* ================= Read ================= */

    public boolean contains(@NonNull String groupId, @NonNull UploadSelection s) {
        return dao.contains(groupId, s.uri.toString(), s.type.name());
    }

    @NonNull
    public List<UploadSelection> getRetriesForGroup(@NonNull String groupId) {
        List<UploadRetryEntity> rows = dao.getByGroup(groupId);
        List<UploadSelection> out = new ArrayList<>(rows.size());
        for (UploadRetryEntity e : rows) out.add(fromEntity(e));
        return out;
    }

    @NonNull
    public Map<String, List<UploadSelection>> getAllRetries() {
        List<UploadRetryEntity> rows = dao.getAll();
        Map<String, List<UploadSelection>> map = new HashMap<>();
        for (UploadRetryEntity e : rows)
            map.computeIfAbsent(e.groupId, k -> new ArrayList<>()).add(fromEntity(e));
        return map;
    }

    public void updateFailureReason(
            @NonNull String groupId,
            @NonNull UploadSelection s,
            @NonNull FailureReason reason
    ) {
        s.failureReason = reason;
        dao.updateFailureReason(
                groupId,
                s.uri.toString(),
                s.type.name(),
                reason.name()
        );
    }


    /* ================= Clear ================= */

    public void clearGroup(@NonNull String groupId) {
        dao.deleteGroup(groupId);
    }

    public void clearAll() {
        dao.deleteAll();
    }

    public void onSessionCleared() {
        Executors.newSingleThreadExecutor().execute(dao::deleteAll);
    }

    /* ================= Mapping ================= */

    private static UploadRetryEntity toEntity(
            @NonNull String groupId,
            @NonNull UploadSelection s
    ) {
        String reason = s.failureReason != null
                ? s.failureReason.name()
                : FailureReason.DRIVE_ERROR.name();

        return new UploadRetryEntity(
                groupId,
                s.uri.toString(),
                s.mimeType,
                s.type.name(),
                s.displayName,
                s.thumbnailPath,
                reason
        );
    }



    private static UploadSelection fromEntity(@NonNull UploadRetryEntity e) {
        UploadSelection s = new UploadSelection(
                Uri.parse(e.uri),
                e.mimeType,              // may be null
                e.displayName,
                -1L,
                System.currentTimeMillis(),
                e.thumbnailPath
        );
        s.failureReason = FailureReason.valueOf(e.failureReason);
        return s;
    }



}
