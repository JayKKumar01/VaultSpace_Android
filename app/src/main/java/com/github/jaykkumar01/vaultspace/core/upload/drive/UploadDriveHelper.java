package com.github.jaykkumar01.vaultspace.core.upload.drive;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.core.upload.UploadQueueEngine;
import com.github.jaykkumar01.vaultspace.core.upload.base.FailureReason;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadType;
import com.github.jaykkumar01.vaultspace.core.upload.base.UploadedItem;
import com.github.jaykkumar01.vaultspace.utils.UriUtils;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ThreadLocalRandom;

public final class UploadDriveHelper {

    private static final String TAG = "VaultSpace:UploadDrive";

    /* -------- Upload tuning (LOCKED) -------- */
    private static final long DIRECT_UPLOAD_MAX = 256L * 1024L;          // ≤256 KB
    private static final long MEDIUM_FILE_MAX = 10L * 1024L * 1024L;     // ≤10 MB

    // NEW (speed-optimized)
    private static final int CHUNK_SMALL  = 2 * 512 * 1024;
    private static final int CHUNK_MEDIUM = 4 * 1024 * 1024;   // 4 MB
    private static final int CHUNK_LARGE  = 8 * 1024 * 1024;   // 8 MB

    public static final class UploadFailure extends Exception {
        public final FailureReason reason;

        public UploadFailure(FailureReason r, String m, Throwable c) {
            super(m, c);
            reason = r;
        }

        public UploadFailure(FailureReason r, String m) {
            this(r, m, null);
        }
    }

    private final Context appContext;
    private final ContentResolver resolver;
    private final TrustedAccountsRepository trustedAccountsRepo;


    public UploadDriveHelper(@NonNull Context context) {
        appContext = context.getApplicationContext();
        resolver = appContext.getContentResolver();
        trustedAccountsRepo = TrustedAccountsRepository.getInstance(context);
    }

    /* ================= Public API ================= */

    public UploadedItem upload(@NonNull String groupId, @NonNull UploadSelection selection,UploadQueueEngine.Callback progress)
            throws UploadFailure, CancellationException {

        Log.d(TAG, "upload start parentId=" + groupId + " uri=" + selection.uri);

        if (!UriUtils.isAccessible(appContext,selection.uri))
            throw new UploadFailure(FailureReason.URI_NOT_FOUND, "Uri not accessible or name missing");

        String email = pickRandomAccount(selection.sizeBytes);
        Drive drive = DriveClientProvider.forAccount(appContext, email);


        String thumbFileId = null;

        if (selection.type == UploadType.VIDEO && selection.thumbnailPath != null) {
            try {
                thumbFileId = uploadVideoThumbnail(drive, selection.thumbnailPath);
            } catch (Exception e) {
                Log.w(TAG, "video thumbnail upload failed, continuing without thumb", e);
            }
        }

        File metadata = new File()
                .setName(selection.displayName)
                .setMimeType(selection.mimeType)
                .setParents(Collections.singletonList(groupId));

        if (thumbFileId != null) {
            metadata.setAppProperties(
                    Collections.singletonMap("thumb", thumbFileId)
            );
        }


        if (selection.momentMillis > 0) {
            metadata.setModifiedTime(new DateTime(selection.momentMillis));
        }


        AbstractInputStreamContent content =
                buildContent(selection.uri, selection.mimeType, selection.sizeBytes);

        UploadedItem item = uploadPreparedFile(groupId, drive, metadata, content, progress, selection.sizeBytes);
        trustedAccountsRepo.recordUploadUsage(email, selection.sizeBytes, null);
        return item;
    }
    /* ================= Utilities ================= */

    private String uploadVideoThumbnail(
            @NonNull Drive drive,
            @NonNull String path
    ) throws Exception {

        String folderId = DriveFolderRepository.getThumbnailsRootId(appContext);
        java.io.File file = new java.io.File(path);

        File meta = new File()
                .setName("vid_thumb_" + System.currentTimeMillis() + ".jpg")
                .setMimeType("image/jpeg")
                .setParents(Collections.singletonList(folderId));

        InputStreamContent content =
                new InputStreamContent("image/jpeg", new java.io.FileInputStream(file));
        content.setLength(file.length()); // 👈 critical: enables true direct upload

        Drive.Files.Create req = drive.files().create(meta, content).setFields("id");
        req.getMediaHttpUploader().setDirectUploadEnabled(true); // 👈 skip resumable infra

        File uploaded = req.execute();

        Log.d(TAG, "thumbnail uploaded id=" + uploaded.getId());
        return uploaded.getId();
    }


    private String pickRandomAccount(long sizeBytes) throws UploadFailure {
        List<TrustedAccount> snapshot =
                trustedAccountsRepo.getAccountsSnapshot();

        if (snapshot.isEmpty()) {
            throw new UploadFailure(
                    FailureReason.NO_TRUSTED_ACCOUNT,
                    "No trusted accounts available"
            );
        }

        List<String> eligible = new ArrayList<>();
        for (TrustedAccount a : snapshot) {
            if (a.totalQuota - a.usedQuota >= sizeBytes) {
                eligible.add(a.email);
            }
        }

        if (eligible.isEmpty()) {
            throw new UploadFailure(
                    FailureReason.NO_SPACE,
                    "No trusted account has enough free space"
            );
        }

        return eligible.get(
                ThreadLocalRandom.current().nextInt(eligible.size())
        );
    }


    private AbstractInputStreamContent buildContent(Uri uri, String mime, long size)
            throws UploadFailure {

        try {
            InputStream in = resolver.openInputStream(uri);
            if (in == null) throw new FileNotFoundException("Null input stream");

            InputStreamContent c = new InputStreamContent(mime, in);
            c.setLength(size);
            return c;

        } catch (FileNotFoundException e) {
            throw new UploadFailure(FailureReason.URI_NOT_FOUND, "Input stream not found", e);
        }
    }

    /* ================= Drive primitive (SMART UPLOAD) ================= */

    private UploadedItem uploadPreparedFile(
            String groupId, @NonNull Drive drive,
            @NonNull File metadata,
            @NonNull AbstractInputStreamContent content,
            @NonNull UploadQueueEngine.Callback listener,
            long fileSize)
            throws UploadFailure, CancellationException {

        try {
            Drive.Files.Create req = drive.files().create(metadata, content);
            req.setFields("id,name,mimeType,size,modifiedTime,thumbnailLink");

            MediaHttpUploader u = req.getMediaHttpUploader();


            if (fileSize <= DIRECT_UPLOAD_MAX) {
                u.setDirectUploadEnabled(true);
            } else {
                u.setDirectUploadEnabled(false);
                u.setChunkSize(
                        fileSize <= MEDIUM_FILE_MAX ? CHUNK_SMALL :
                                fileSize <= 100L * 1024L * 1024L ? CHUNK_MEDIUM :
                                        CHUNK_LARGE
                );
            }

            u.setProgressListener(p -> {
                if (Thread.currentThread().isInterrupted()) throw new CancellationException();
                listener.onProgress(groupId, metadata.getName(), p.getNumBytesUploaded(), content.getLength());
            });

            File f = req.execute();

            UploadedItem item = new UploadedItem(
                    f.getId(),
                    f.getName(),
                    f.getMimeType(),
                    f.getSize() != null ? f.getSize() : 0L,
                    f.getModifiedTime() != null ? f.getModifiedTime().getValue() : System.currentTimeMillis(),
                    f.getThumbnailLink()
            );

            Log.d(TAG,
                    "\nUpload successful\n" +
                            "────────────────────────────────\n" +
                            "File name   : " + item.name + "\n" +
                            "File ID     : " + item.fileId + "\n" +
                            "Type        : " + item.getType() + "\n" +
                            "MIME        : " + item.mimeType + "\n" +
                            "Size        : " + formatSize(item.sizeBytes) + "\n" +
                            "Modified    : " + formatTime(item.modifiedTime) + "\n" +
                            "Thumbnail   : " + (item.thumbnailLink != null ? "available" : "none") + "\n" +
                            "────────────────────────────────"
            );


            return item;

        } catch (HttpResponseException e) {

            int code = e.getStatusCode();
            if (code == 401 || code == 403)
                throw new UploadFailure(FailureReason.NO_ACCESS, "Drive permission denied", e);

            throw new UploadFailure(FailureReason.DRIVE_ERROR, "Drive HTTP error " + code, e);

        } catch (CancellationException e) {
            throw e;
        } catch (IOException e) {
            throw new UploadFailure(FailureReason.IO_ERROR, "IO error during upload", e);
        } catch (Exception e) {
            throw new UploadFailure(FailureReason.DRIVE_ERROR, "Drive upload failed", e);
        }
    }


    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024) + " KB";
        return String.format("%.2f MB", bytes / (1024f * 1024f));
    }

    private static String formatTime(long millis) {
        return java.text.DateFormat
                .getDateTimeInstance(
                        java.text.DateFormat.MEDIUM,
                        java.text.DateFormat.SHORT
                )
                .format(new java.util.Date(millis));
    }

    public void release() {
    }
}
