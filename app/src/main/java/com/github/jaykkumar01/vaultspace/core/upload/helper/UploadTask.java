package com.github.jaykkumar01.vaultspace.core.upload.helper;

import com.github.jaykkumar01.vaultspace.core.upload.base.*;
import com.github.jaykkumar01.vaultspace.core.upload.drive.UploadDriveHelper;

import java.util.UUID;
import java.util.concurrent.CancellationException;

public final class UploadTask implements Runnable {

    public interface Callback{
        void onSuccess(String groupId, UploadSelection s, UploadedItem item);
        void onFailure(String groupId, UploadSelection s, FailureReason r);
        void onProgress(String uploadId,String groupId, String name, long uploaded, long total);
    }

    final String uploadId;
    final String groupId;
    final UploadSelection selection;
    private final UploadDriveHelper helper;
    private final Callback cb;

    public UploadTask(String groupId, UploadSelection s, UploadDriveHelper h, Callback cb){
        this.uploadId = UUID.randomUUID().toString();
        this.groupId=groupId;
        this.selection=s;
        this.helper=h;
        this.cb=cb;
    }

    @Override public void run(){
        if(Thread.currentThread().isInterrupted()) return;
        try{
            UploadedItem item = helper.upload(
                    groupId,
                    selection,
                    (u,t)->cb.onProgress(uploadId,groupId,selection.displayName,u,t)
            );
            cb.onSuccess(groupId,selection,item);
        }catch(UploadDriveHelper.UploadFailure f){
            cb.onFailure(groupId,selection,f.reason);
        }catch(CancellationException ignored){}
    }
}
