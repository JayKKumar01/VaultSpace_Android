package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureDao;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureDatabase;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;

import java.util.List;
import java.util.concurrent.Executors;

public final class UploadFailureStore {

    private final UploadFailureDao dao;

    public UploadFailureStore(@NonNull Context context) {
        this.dao = UploadFailureDatabase.get(context).dao();
    }

    /* ================= Write ================= */

    public void addFailure(@NonNull UploadFailureEntity e) {
        dao.insert(e);
    }

    public void addFailures(@NonNull List<UploadFailureEntity> list) {
        if (list.isEmpty()) return;
        dao.insertAll(list);
    }

    /* ================= Read ================= */

    public List<UploadFailureEntity> getFailuresForGroup(@NonNull String groupId) {
        return dao.getByGroup(groupId);
    }

    public boolean contains(
            @NonNull String groupId,
            @NonNull String uri,
            @NonNull String type
    ) {
        return dao.contains(groupId, uri, type);
    }

    public UploadFailureEntity getFailureByUri(@NonNull String uri) {
        return dao.getByUri(uri);
    }


    /* ================= Delete ================= */

    public void removeFailure(
            @NonNull String groupId,
            @NonNull String uri,
            @NonNull String type
    ) {
        dao.delete(groupId, uri, type);
    }

    public void removeFailuresByUris(
            @NonNull String groupId,
            @NonNull List<String> uris
    ) {
        if (uris.isEmpty()) return;
        dao.deleteByUris(groupId, uris);
    }

    public void clearGroup(@NonNull String groupId) {
        dao.deleteByGroup(groupId);
    }

    public void clearAll() {
        dao.deleteAll();
    }

    public void onSessionCleared() {
        Executors.newSingleThreadExecutor().execute(dao::deleteAll);
    }
}
