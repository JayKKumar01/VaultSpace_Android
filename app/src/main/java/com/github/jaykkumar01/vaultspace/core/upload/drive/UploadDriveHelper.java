package com.github.jaykkumar01.vaultspace.core.upload.drive;

import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;

import java.util.concurrent.CancellationException;

public final class UploadDriveHelper {

    public enum FailureReason {

        NO_TRUSTED_ACCOUNT(true),
        NO_ACCESS(true),
        NO_SPACE(true),
        URI_NOT_FOUND(false),
        IO_ERROR(true),
        DRIVE_ERROR(true),
        CANCELLED(false);

        private final boolean retryable;

        FailureReason(boolean retryable){
            this.retryable = retryable;
        }

        public boolean isRetryable(){
            return retryable;
        }
    }


    public static final class UploadFailure extends Exception {

        public final FailureReason reason;

        public UploadFailure(FailureReason reason,String message,Throwable cause){
            super(message,cause);
            this.reason=reason;
        }

        public UploadFailure(FailureReason reason,String message){
            this(reason,message,null);
        }
    }


    private final Context appContext;

    public UploadDriveHelper(@NonNull Context context){
        appContext=context.getApplicationContext();
    }

    public UploadedItem upload(
            @NonNull String groupId,
            @NonNull UploadSelection selection
    ) throws UploadFailure,CancellationException {
        throw new UnsupportedOperationException("Not implemented");
    }

    // Steps:
    // 1. Resolve trusted accounts
    // 2. Re-check writer access
    // 3. Filter by storage
    // 4. Random select account
    // 5. Build Drive client
    // 6. Stream upload
    // 7. Map failures
}
