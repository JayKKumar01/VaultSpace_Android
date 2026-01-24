package com.github.jaykkumar01.vaultspace.core.upload.drive;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.core.drive.TrustedAccountsRepository;
import com.github.jaykkumar01.vaultspace.core.upload.base.*;
import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.github.jaykkumar01.vaultspace.utils.UriUtils;
import com.google.api.client.googleapis.media.MediaHttpUploader;
import com.google.api.client.http.*;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class UploadDriveHelper {

    private static final String TAG = "VaultSpace:UploadDrive";

    private static final int CHUNK = MediaHttpUploader.MINIMUM_CHUNK_SIZE;

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
    private final ConcurrentHashMap<String, Drive> driveCache = new ConcurrentHashMap<>();

    public UploadDriveHelper(@NonNull Context context) {
        appContext = context.getApplicationContext();
        resolver = appContext.getContentResolver();
        trustedAccountsRepo = TrustedAccountsRepository.getInstance(context);
    }

    private Drive getDrive(String email) {
        return driveCache.computeIfAbsent(
                email, e -> DriveClientProvider.forAccount(appContext, e)
        );
    }

    /* ================= Public API ================= */

    public UploadedItem upload(
            String parentId,
            UploadSelection selection,
            ProgressCallback cb
    ) throws UploadFailure, CancellationException {

        Log.d(TAG, "upload start parentId=" + parentId + " uri=" + selection.uri);

        if (!UriUtils.isAccessible(appContext, selection.uri))
            throw new UploadFailure(FailureReason.URI_NOT_FOUND, "Uri not accessible");

        String email = pickRandomAccount(selection.sizeBytes);
        Drive drive = getDrive(email);

        String thumbFileId = null;
        if (selection.type == UploadType.VIDEO && selection.thumbnailPath != null) {
            try {
                thumbFileId = uploadVideoThumbnail(drive, selection.thumbnailPath);
            } catch (Exception e) {
                Log.w(TAG, "thumbnail upload failed, continuing", e);
            }
        }

        File meta = new File()
                .setName(selection.displayName)
                .setMimeType(selection.mimeType)
                .setParents(Collections.singletonList(parentId));

        if (thumbFileId != null)
            meta.setAppProperties(Collections.singletonMap("thumb", thumbFileId));

        if (selection.momentMillis > 0)
            meta.setModifiedTime(new DateTime(selection.momentMillis));

        InputStream in = null;
        try {
            AbstractInputStreamContent content =
                    buildContent(selection.uri, selection.mimeType, selection.sizeBytes);

            in = ((InputStreamContent) content).getInputStream();

            UploadedItem item = uploadPreparedFile(drive, meta, content, cb, selection.sizeBytes);

            trustedAccountsRepo.recordUploadUsage(email, selection.sizeBytes, null);
            return item;

        } finally {
            closeQuietly(in);
        }
    }

    /* ================= Utilities ================= */

    private String uploadVideoThumbnail(Drive drive, String path) throws Exception {

        String folderId = DriveFolderRepository.getThumbnailsRootId(appContext);
        java.io.File file = new java.io.File(path);

        File meta = new File()
                .setName("vid_thumb_" + System.currentTimeMillis() + ".jpg")
                .setMimeType("image/jpeg")
                .setParents(Collections.singletonList(folderId));

        InputStreamContent content =
                new InputStreamContent("image/jpeg", new FileInputStream(file));
        content.setLength(file.length());

        Drive.Files.Create req =
                drive.files().create(meta, content).setFields("id");
        req.getMediaHttpUploader().setDirectUploadEnabled(true);

        return req.execute().getId();
    }

    private String pickRandomAccount(long sizeBytes) throws UploadFailure {

        List<TrustedAccount> snapshot = trustedAccountsRepo.getAccountsSnapshot();
        if (snapshot.isEmpty())
            throw new UploadFailure(FailureReason.NO_TRUSTED_ACCOUNT, "No trusted accounts");

        List<String> eligible = new ArrayList<>();
        for (TrustedAccount a : snapshot)
            if (a.totalQuota - a.usedQuota >= sizeBytes)
                eligible.add(a.email);

        if (eligible.isEmpty())
            throw new UploadFailure(FailureReason.NO_SPACE, "No account has space");

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
            throw new UploadFailure(
                    FailureReason.URI_NOT_FOUND,
                    "Input stream not found",
                    e
            );
        }
    }

    /* ================= Drive primitive ================= */

    private UploadedItem uploadPreparedFile(
            Drive drive,
            File meta,
            AbstractInputStreamContent content,
            ProgressCallback cb,
            long fileSize
    ) throws UploadFailure, CancellationException {

        try {
            Drive.Files.Create req = drive.files().create(meta, content);
            req.setFields("id,name,mimeType,size,modifiedTime,thumbnailLink");

            MediaHttpUploader u = req.getMediaHttpUploader();

            if (fileSize <= CHUNK) {
                u.setDirectUploadEnabled(true);
            } else {
                u.setDirectUploadEnabled(false);
                u.setChunkSize(CHUNK);
            }

            u.setProgressListener(p -> {
                if (Thread.currentThread().isInterrupted())
                    throw new CancellationException();
                cb.onProgress(p.getNumBytesUploaded(), content.getLength());
            });

            File f = req.execute();

            return new UploadedItem(
                    f.getId(),
                    f.getName(),
                    f.getMimeType(),
                    f.getSize() != null ? f.getSize() : 0L,
                    f.getModifiedTime() != null
                            ? f.getModifiedTime().getValue()
                            : System.currentTimeMillis(),
                    f.getThumbnailLink()
            );

        } catch (HttpResponseException e) {
            int c = e.getStatusCode();
            if (c == 401 || c == 403)
                throw new UploadFailure(FailureReason.NO_ACCESS, "Drive permission denied", e);
            throw new UploadFailure(FailureReason.DRIVE_ERROR, "Drive HTTP " + c, e);

        } catch (IOException e) {
            throw new UploadFailure(FailureReason.IO_ERROR, "IO error", e);
        } catch (CancellationException e) {
            throw e;
        } catch (Exception e) {
            throw new UploadFailure(FailureReason.DRIVE_ERROR, "Drive upload failed", e);
        }
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) {
            try {
                in.close();
            } catch (IOException ignored) {
            }
        }
    }
}
