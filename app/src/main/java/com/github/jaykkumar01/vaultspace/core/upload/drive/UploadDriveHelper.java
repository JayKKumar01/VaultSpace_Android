package com.github.jaykkumar01.vaultspace.core.upload.drive;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.session.UploadFailureStore;
import com.github.jaykkumar01.vaultspace.core.session.UserSession;
import com.github.jaykkumar01.vaultspace.core.session.cache.TrustedAccountsCache;
import com.github.jaykkumar01.vaultspace.core.session.db.UploadFailureEntity;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.models.UriFileInfo;
import com.github.jaykkumar01.vaultspace.models.base.UploadSelection;
import com.github.jaykkumar01.vaultspace.models.base.UploadType;
import com.github.jaykkumar01.vaultspace.models.base.UploadedItem;
import com.github.jaykkumar01.vaultspace.utils.UploadThumbnailGenerator;
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
    private static final int CHUNK_SMALL = MediaHttpUploader.MINIMUM_CHUNK_SIZE; // 256 KB
    private static final int CHUNK_MEDIUM = 512 * 1024;              // 512 KB
    private static final int CHUNK_LARGE = 1024 * 1024;             // 1 MB
    private final Drive primaryDrive;

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
    private final TrustedAccountsCache trustedCache;

    public UploadDriveHelper(@NonNull Context context) {
        appContext = context.getApplicationContext();
        resolver = appContext.getContentResolver();
        UserSession userSession = new UserSession(appContext);
        this.primaryDrive = DriveClientProvider.forAccount(appContext,userSession.getPrimaryAccountEmail());
        trustedCache = userSession.getVaultCache().trustedAccounts;
    }

    /* ================= Public API ================= */

    public UploadedItem upload(@NonNull String groupId, @NonNull UploadSelection selection, UploadFailureStore failureStore, java.io.File thumbDir)
            throws UploadFailure, CancellationException {

        Log.d(TAG, "upload start parentId=" + groupId + " uri=" + selection.uri);

        UriFileInfo info = UriUtils.resolve(appContext, selection.uri);
        if (info.name.isEmpty())
            throw new UploadFailure(FailureReason.URI_NOT_FOUND, "Uri not accessible or name missing");

        String email = pickRandomAccount(info.sizeBytes);
        Drive drive = DriveClientProvider.forAccount(appContext, email);


        String thumbFileId = null;

        if (selection.getType() == UploadType.VIDEO) {
            try {
                thumbFileId = uploadVideoThumbnail(drive, selection.uri, failureStore, thumbDir);
            } catch (Exception e) {
                Log.w(TAG, "video thumbnail upload failed, continuing without thumb", e);
            }
        }

        File metadata = new File()
                .setName(info.name)
                .setMimeType(selection.mimeType)
                .setParents(Collections.singletonList(groupId));

        if (thumbFileId != null) {
            metadata.setAppProperties(
                    Collections.singletonMap("thumb", thumbFileId)
            );
        }


        if (info.modifiedTimeMillis > 0) {
            metadata.setModifiedTime(new DateTime(info.modifiedTimeMillis));
        }


        AbstractInputStreamContent content =
                buildContent(selection.uri, selection.mimeType, info.sizeBytes);

        UploadProgressListener progress = (u, t) ->
                Log.d(TAG, "progress parentId=" + groupId +
                        " uploaded=" + u +
                        " total=" + t +
                        " account=" + email);

        return uploadPreparedFile(drive, metadata, content, progress, info.sizeBytes);
    }

    /* ================= Utilities ================= */

    private String uploadVideoThumbnail(
            @NonNull Drive drive,
            @NonNull Uri videoUri,
            @NonNull UploadFailureStore failureStore,
            @NonNull java.io.File thumbDir
    ) throws Exception {
        UploadFailureEntity f = failureStore.getFailureByUri(videoUri.toString());
        String path = f != null ? f.thumbnailPath : null;

        if (path == null)
            path = UploadThumbnailGenerator.generate(appContext, videoUri, UploadType.VIDEO, thumbDir);

        if (path == null) {
            Log.w(TAG, "thumbnail generation failed");
            return null;
        }

        String folderId = DriveFolderRepository.getThumbnailsRootId(primaryDrive);
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
        List<String> eligible = new ArrayList<>();
        boolean hasAny = false;

        for (TrustedAccount a : trustedCache.getAccountsView()) {
            hasAny = true;
            if (a.totalQuota - a.usedQuota >= sizeBytes) eligible.add(a.email);
        }

        if (eligible.isEmpty())
            throw new UploadFailure(
                    hasAny ? FailureReason.NO_SPACE : FailureReason.NO_TRUSTED_ACCOUNT,
                    "No eligible trusted account"
            );

        return eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
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
            @NonNull Drive drive,
            @NonNull File metadata,
            @NonNull AbstractInputStreamContent content,
            @NonNull UploadProgressListener listener,
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
                listener.onProgress(p.getNumBytesUploaded(), content.getLength());
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

}
