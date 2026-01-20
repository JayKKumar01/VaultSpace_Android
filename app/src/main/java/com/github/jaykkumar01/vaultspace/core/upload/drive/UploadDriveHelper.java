package com.github.jaykkumar01.vaultspace.core.upload.drive;


import android.content.Context;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.CancellationException;


public final class UploadDriveHelper {

    public interface UploadProgressListener {
        void onProgress(long uploadedBytes, long totalBytes);
    }

    public enum FailureReason {

        NO_TRUSTED_ACCOUNT(true),
        NO_ACCESS(true),
        NO_SPACE(true),
        URI_NOT_FOUND(false),
        IO_ERROR(true),
        DRIVE_ERROR(true),
        CANCELLED(false);

        private final boolean retryable;

        FailureReason(boolean retryable) {
            this.retryable = retryable;
        }

        public boolean isRetryable() {
            return retryable;
        }
    }


    public static final class UploadFailure extends Exception {

        public final FailureReason reason;

        public UploadFailure(FailureReason reason, String message, Throwable cause) {
            super(message, cause);
            this.reason = reason;
        }

        public UploadFailure(FailureReason reason, String message) {
            this(reason, message, null);
        }
    }


    private final Context appContext;

    public UploadDriveHelper(@NonNull Context context) {
        appContext = context.getApplicationContext();
    }

    public UploadedItem upload(
            @NonNull String groupId,
            @NonNull UploadSelection selection
    ) throws UploadFailure,CancellationException{

        try{
            Thread.sleep(2000);
        }catch(InterruptedException e){
            throw new CancellationException();
        }

        if(Math.random()<0.5){
            long now=System.currentTimeMillis();
            return new UploadedItem(
                    "fake_"+now,
                    "Upload_"+now,
                    selection.mimeType,
                    1_000_000L,
                    now,
                    null
            );
        }

        FailureReason reason=
                Math.random()<0.5
                        ?FailureReason.URI_NOT_FOUND
                        :FailureReason.DRIVE_ERROR;

        throw new UploadFailure(
                reason,
                "Simulated upload failure: "+reason
        );
    }


    /* ================= Utility ================= */
    private UploadedItem uploadPreparedFile(@NonNull Drive drive, @NonNull File metadata, @NonNull AbstractInputStreamContent content, @NonNull UploadProgressListener listener) throws UploadFailure, CancellationException {

        try {
            Drive.Files.Create request = drive.files().create(metadata, content);
            request.setFields("id,name,mimeType,size,modifiedTime,thumbnailLink");

            MediaHttpUploader uploader = request.getMediaHttpUploader();
            uploader.setDirectUploadEnabled(false);
            uploader.setChunkSize(MediaHttpUploader.MINIMUM_CHUNK_SIZE);

            uploader.setProgressListener(p -> {
                if (Thread.currentThread().isInterrupted())
                    throw new CancellationException();
                listener.onProgress(p.getNumBytesUploaded(), content.getLength());
            });

            File uploaded = request.execute();

            return new UploadedItem(
                    uploaded.getId(),
                    uploaded.getName(),
                    uploaded.getMimeType(),
                    uploaded.getSize() != null ? uploaded.getSize() : 0L,
                    uploaded.getModifiedTime() != null
                            ? uploaded.getModifiedTime().getValue()
                            : System.currentTimeMillis(),
                    uploaded.getThumbnailLink()
            );

        } catch (CancellationException e) {
            throw e;
        } catch (FileNotFoundException e) {
            throw new UploadFailure(FailureReason.URI_NOT_FOUND, "Input stream not found", e);
        } catch (IOException e) {
            throw new UploadFailure(FailureReason.IO_ERROR, "IO error during upload", e);
        } catch (Exception e) {
            throw new UploadFailure(FailureReason.DRIVE_ERROR, "Drive upload failed", e);
        }
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
