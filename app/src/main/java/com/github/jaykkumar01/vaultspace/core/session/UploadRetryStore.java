package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;
import android.net.Uri;
import androidx.annotation.NonNull;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadRetryDao;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadRetryDatabase;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadRetryEntity;
import com.github.jaykkumar01.vaultspace.core.upload.base.*;
import java.util.*;
import java.util.concurrent.Executors;

public final class UploadRetryStore {

    private final UploadRetryDao dao;

    public UploadRetryStore(@NonNull Context context) {
        this.dao = UploadRetryDatabase.get(context).dao();
    }

    /* ================= Write ================= */

    public void addRetry(@NonNull UploadSelection s) {
        dao.insert(toEntity(s));
    }

    public void addRetryBatch(@NonNull List<UploadSelection> selections) {
        if (selections.isEmpty()) return;
        List<UploadRetryEntity> list = new ArrayList<>(selections.size());
        for (UploadSelection s : selections) list.add(toEntity(s));
        dao.insertAll(list);
    }

    public void removeRetry(@NonNull UploadSelection s) {
        dao.deleteById(s.id);
    }

    /* ================= Read ================= */

    public boolean contains(@NonNull String uploadId) {
        return dao.containsId(uploadId);
    }

    @NonNull
    public List<UploadSelection> getRetriesForGroup(@NonNull String groupId) {
        List<UploadRetryEntity> rows = dao.getByGroup(groupId);
        List<UploadSelection> out = new ArrayList<>(rows.size());
        for (UploadRetryEntity e : rows) out.add(fromEntity(e));
        return out;
    }

    @NonNull
    public Map<String,List<UploadSelection>> getAllRetries() {
        List<UploadRetryEntity> rows = dao.getAll();
        Map<String,List<UploadSelection>> map = new HashMap<>();
        for (UploadRetryEntity e : rows)
            map.computeIfAbsent(e.groupId,k->new ArrayList<>()).add(fromEntity(e));
        return map;
    }

    public void updateFailureReason(@NonNull UploadSelection s,@NonNull FailureReason r) {
        dao.updateFailureReason(s.id, r.name());
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

    private static UploadRetryEntity toEntity(@NonNull UploadSelection s) {
        UploadContext c = s.context;
        String reason = c.failureReason != null ? c.failureReason.name() : FailureReason.DRIVE_ERROR.name();
        return new UploadRetryEntity(
                s.id,
                c.groupId,
                s.uri.toString(),
                s.mimeType,
                s.displayName,
                s.sizeBytes,
                s.momentMillis,
                s.thumbnailPath,
                reason
        );
    }

    private static UploadSelection fromEntity(@NonNull UploadRetryEntity e) {
        UploadSelection s = new UploadSelection(
                e.id,
                e.groupId,
                Uri.parse(e.uri),
                e.mimeType,
                e.displayName,
                e.sizeBytes,
                e.momentMillis,
                e.thumbnailPath
        );
        s.context.failureReason = FailureReason.valueOf(e.failureReason);
        return s;
    }
}
