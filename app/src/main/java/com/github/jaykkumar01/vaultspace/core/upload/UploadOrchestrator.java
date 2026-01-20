package com.github.jaykkumar01.vaultspace.core.upload;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.github.jaykkumar01.vaultspace.album.AlbumUploadSideEffect;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.UploadCache;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class UploadOrchestrator {

    /* ==========================================================
     * Constants
     * ========================================================== */

    private static final String TAG = "VaultSpace:UploadOrchestrator";
    private static UploadOrchestrator INSTANCE;

    /* ==========================================================
     * Fields
     * ========================================================== */

    private final Context appContext;
    private final UploadManager uploadManager;
    private final UploadCache uploadCache;
    private final List<UploadSideEffect> sideEffects = new ArrayList<>();

    /* ==========================================================
     * Service state
     * ========================================================== */

    private enum ServiceState {
        IDLE,
        RUNNING,
        FINALIZING
    }

    private ServiceState serviceState = ServiceState.IDLE;

    /* ==========================================================
     * Singleton
     * ========================================================== */

    public static synchronized UploadOrchestrator getInstance(
            @NonNull Context context
    ){
        if(INSTANCE==null){
            INSTANCE=new UploadOrchestrator(context);
        }
        return INSTANCE;
    }

    /* ==========================================================
     * Constructor
     * ========================================================== */

    private UploadOrchestrator(@NonNull Context context){
        this.appContext=context.getApplicationContext();

        UserSession session=new UserSession(appContext);
        this.uploadCache=session.getVaultCache().uploadCache;

        this.uploadManager=new UploadManager(appContext);
        this.uploadManager.attachOrchestrator(this);

        registerSideEffect(new AlbumUploadSideEffect(appContext));

        Log.d(TAG,"Initialized. serviceState=IDLE");
    }

    /* ==========================================================
     * UI-facing API
     * ========================================================== */

    public void enqueue(
            @NonNull String groupId,
            @NonNull String groupLabel,
            @NonNull List<UploadSelection> selections
    ){
        uploadManager.enqueue(groupId,groupLabel,selections);
    }

    public void registerObserver(
            @NonNull String groupId,
            @NonNull String groupLabel,
            @NonNull UploadObserver observer
    ){
        uploadManager.registerObserver(groupId,groupLabel,observer);
    }

    public void unregisterObserver(@NonNull String groupId){
        uploadManager.unregisterObserver(groupId);
    }

    public void retryUploads(@NonNull String groupId,@NonNull String groupName){
        uploadManager.retry(groupId,groupName);
    }

    public void cancelUploads(@NonNull String groupId){
        uploadManager.cancelUploads(groupId);
    }

    public void cancelAllUploads(){
        uploadManager.cancelAllUploads();
    }

    public void clearGroup(String groupId){
        uploadManager.clearGroup(groupId);
    }

    public void getFailuresForGroup(
            @NonNull String groupId,
            @NonNull Consumer<List<UploadFailureEntity>> cb
    ){
        uploadManager.getFailuresForGroup(groupId,cb);
    }

    /* ==========================================================
     * Upload side-effect dispatch
     * ========================================================== */

    private void registerSideEffect(@NonNull UploadSideEffect effect){
        sideEffects.add(effect);
    }

    public void dispatchUploadSuccess(String groupId, UploadedItem item){
        for(UploadSideEffect e:sideEffects)
            e.onUploadSuccess(groupId,item);
    }

    public void dispatchUploadFailure(String groupId, UploadSelection sel){
        for(UploadSideEffect e:sideEffects)
            e.onUploadFailure(groupId,sel);
    }

    /* ==========================================================
     * UploadManager → Orchestrator callback
     * ========================================================== */

    public void onUploadStateChanged(){

        boolean hasActive=uploadCache.hasAnyActiveUploads();

        switch(serviceState){

            case IDLE:
                if(hasActive){
                    startForegroundService();
                    serviceState=ServiceState.RUNNING;
                }
                return;

            case RUNNING:
                nudgeForegroundService();
                return;

            case FINALIZING:
                return;
        }
    }

    /* ==========================================================
     * Foreground service helpers
     * ========================================================== */

    private void startForegroundService(){
        Intent intent=new Intent(appContext,UploadForegroundService.class);
        ContextCompat.startForegroundService(appContext,intent);
    }

    private void nudgeForegroundService(){
        Intent intent=new Intent(appContext,UploadForegroundService.class);
        appContext.startService(intent);
    }

    private void stopForegroundServiceImmediately(){
        Intent intent=new Intent(appContext,UploadForegroundService.class);
        appContext.stopService(intent);
        serviceState=ServiceState.IDLE;
    }

    /* ==========================================================
     * Service lifecycle callbacks
     * ========================================================== */

    public void onServiceFinalizing(){
        serviceState=ServiceState.FINALIZING;
    }

    public void onServiceDestroyed(){
        serviceState=ServiceState.IDLE;
    }

    /* ==========================================================
     * Session cleanup
     * ========================================================== */

    public void onSessionCleared(){

        uploadManager.cancelAllUploads();

        Intent intent=new Intent(appContext,UploadForegroundService.class);
        appContext.stopService(intent);

        serviceState=ServiceState.IDLE;
    }
}
