package com.github.jaykkumar01.vaultspace.core.session;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureDao;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureDatabase;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class UploadFailureStore {

    private final UploadFailureDao dao;
    private final Executor executor = Executors.newSingleThreadExecutor();

    public UploadFailureStore(@NonNull Context context){
        this.dao = UploadFailureDatabase.get(context).dao();
    }

    /* ================= Write ================= */

    public void addFailure(@NonNull UploadFailureEntity entity){
        executor.execute(() -> dao.insert(entity));
    }

    public void addFailures(@NonNull List<UploadFailureEntity> entities){
        executor.execute(() -> dao.insertAll(entities));
    }

    /* ================= Read ================= */

    public void getFailuresForGroup(
            @NonNull String groupId,
            @NonNull Consumer<List<UploadFailureEntity>> callback
    ){
        executor.execute(() -> callback.accept(dao.getByGroup(groupId)));
    }

    /* ================= Delete ================= */

    public void removeFailure(
            @NonNull String groupId,
            @NonNull String uri,
            @NonNull String type
    ){
        executor.execute(() -> dao.delete(groupId, uri, type));
    }

    public void removeFailuresByUris(
            @NonNull String groupId,
            @NonNull List<String> uris
    ){
        executor.execute(() -> dao.deleteByUris(groupId, uris));
    }

    public void clearGroup(@NonNull String groupId){
        executor.execute(() -> dao.deleteByGroup(groupId));
    }

    public void clearAll(){
        executor.execute(dao::deleteAll);
    }
}
