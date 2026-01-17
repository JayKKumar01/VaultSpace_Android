package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class UploadManager {

    private static final String TAG="VaultSpace:UploadManager";

    private final Context appContext;
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    private final Handler mainHandler=new Handler(Looper.getMainLooper());
    private final Map<String,UploadObserver> observers=new ConcurrentHashMap<>();
    private UploadOrchestrator orchestrator;

    public UploadManager(@NonNull Context context){
        this.appContext=context.getApplicationContext();
        Log.d(TAG,"Initialized (skeleton)");
    }

    public void attachOrchestrator(@NonNull UploadOrchestrator orchestrator){
        this.orchestrator=orchestrator;
    }

    private void notifyStateChanged(){
        if(orchestrator!=null) mainHandler.post(orchestrator::onUploadStateChanged);
    }

    /* ================= Observer ================= */

    public void registerObserver(@NonNull String groupId,@NonNull String groupName,@NonNull UploadObserver observer){
        Log.d(TAG,"registerObserver(): groupId="+groupId);
        executor.execute(()->observers.put(groupId,observer));
    }

    public void unregisterObserver(@NonNull String groupId){
        Log.d(TAG,"unregisterObserver(): groupId="+groupId);
        executor.execute(()->observers.remove(groupId));
    }

    /* ================= Enqueue (stub) ================= */

    public void enqueue(@NonNull String groupId,@NonNull String groupName,@NonNull List<? extends UploadSelection> selections){
        Log.d(TAG,"enqueue(): groupId="+groupId+", items="+selections.size());
        executor.execute(this::notifyStateChanged);
    }

    /* ================= Cancel (stub) ================= */

    public void cancelUploads(@NonNull String groupId){
        Log.d(TAG,"cancelUploads(): groupId="+groupId);
        executor.execute(this::notifyStateChanged);
    }

    public void cancelAllUploads(){
        Log.d(TAG,"cancelAllUploads()");
        executor.execute(this::notifyStateChanged);
    }

    public void shutdown(){
        Log.d(TAG,"shutdown()");
        executor.shutdownNow();
    }
}
