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
import com.github.jaykkumar01.vaultspace.core.upload.helper.CancelToken;
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
            ProgressCallback cb,
            CancelToken token
    ) throws UploadFailure, CancellationException {

        Log.d(TAG, "upload start parentId=" + parentId + " uri=" + selection.uri);

        if (UriUtils.isPermissionRevoked(appContext, selection.uri))
            throw new UploadFailure(FailureReason.URI_NOT_FOUND, "Uri not accessible");

        String email = pickRandomAccount(selection.sizeBytes);
        Drive drive = getDrive(email);

        String thumbFileId = null;
        if (selection.thumbnailPath != null) {
            try {
                thumbFileId = uploadThumbnail(drive, selection.thumbnailPath);
            } catch (Exception e) {
                Log.w(TAG, "thumbnail upload failed, continuing", e);
            }
        }


        File meta = new File()
                .setName(selection.displayName)
                .setMimeType(selection.mimeType)
                .setParents(Collections.singletonList(parentId));

        Map<String, String> appProps = new HashMap<>();

        if (thumbFileId != null)
            appProps.put("thumb", thumbFileId);

        // 🔑 layout-critical metadata (NEW)
        appProps.put("vs_aspect_ratio", Float.toString(selection.aspectRatio));
        appProps.put("vs_rotation", Integer.toString(selection.rotation));


        long origin = selection.originMoment;
        long moment = selection.momentMillis;

        DateTime created;
        if (origin > 0) {
            created = new DateTime(origin);
            appProps.put("vs_created_source", "origin");
        } else {
            created = new DateTime(moment);
            appProps.put("vs_created_source", "moment");
        }

        meta.setCreatedTime(created);
        meta.setModifiedTime(new DateTime(moment));

        if (!appProps.isEmpty())
            meta.setAppProperties(appProps);


        InputStream in = null;
        String safeMime = selection.mimeType != null ? selection.mimeType : "application/octet-stream";

        try {
            AbstractInputStreamContent content = buildContent(selection.uri, safeMime, selection.sizeBytes, token);

            in = ((InputStreamContent) content).getInputStream();

            UploadedItem item = uploadPreparedFile(drive, meta, thumbFileId, content, cb, selection, token);

            trustedAccountsRepo.recordUploadUsage(email, selection.sizeBytes);
            return item;

        } finally {
            closeQuietly(in);
        }
    }

    /* ================= Utilities ================= */

    private AbstractInputStreamContent buildContent(Uri uri, String mime, long size, CancelToken token) throws UploadFailure {

        try {
            InputStream raw = resolver.openInputStream(uri);
            if (raw == null) throw new FileNotFoundException("Null input stream");

            InputStream in = new CancellableInputStream(raw, token);
            InputStreamContent c = new InputStreamContent(mime, in);
            c.setLength(size);
            return c;

        } catch (FileNotFoundException e) {
            throw new UploadFailure(FailureReason.URI_NOT_FOUND, "Input stream not found", e);
        }
    }

    private UploadedItem uploadPreparedFile(
            Drive drive,
            File meta,
            String thumbFileId,
            AbstractInputStreamContent content,
            ProgressCallback cb,
            UploadSelection selection,
            CancelToken token
    ) throws UploadFailure, CancellationException {

        try {
            Drive.Files.Create req = drive.files().create(meta, content);
            req.setFields("id,name,mimeType,createdTime,modifiedTime,size");

            MediaHttpUploader u = req.getMediaHttpUploader();
            u.setDirectUploadEnabled(selection.sizeBytes <= CHUNK);
            if (selection.sizeBytes > CHUNK) u.setChunkSize(CHUNK);

            u.setProgressListener(p -> {
                if (token.isCancelled())
                    throw new CancellationException();

                long total = content.getLength() > 0 ? content.getLength() : selection.sizeBytes;
                cb.onProgress(p.getNumBytesUploaded(), total);
            });

            File f = req.execute();
            Log.d(TAG, "thumb=" + (thumbFileId != null ? thumbFileId : "none"));


            long originMoment =
                    f.getCreatedTime() != null
                            ? f.getCreatedTime().getValue()
                            : selection.originMoment;

            long momentMillis =
                    f.getModifiedTime() != null
                            ? f.getModifiedTime().getValue()
                            : selection.momentMillis;

            boolean vsOrigin = selection.originMoment > 0;

            return new UploadedItem(
                    f.getId(),
                    f.getName(),
                    f.getMimeType(),
                    f.getSize() != null ? f.getSize() : 0L,
                    originMoment,
                    momentMillis,
                    vsOrigin,
                    selection.aspectRatio,   // 🟢 NEW
                    selection.rotation,      // 🟢 NEW
                    thumbFileId
            );


        } catch (HttpResponseException e) {
            int c = e.getStatusCode();
            if (c == 401 || c == 403)
                throw new UploadFailure(FailureReason.NO_ACCESS, "Drive permission denied", e);
            throw new UploadFailure(FailureReason.DRIVE_ERROR, "Drive HTTP " + c, e);

        } catch (IOException e) {
            throw new UploadFailure(FailureReason.IO_ERROR, "IO error", e);
        }
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) try {
            in.close();
        } catch (IOException ignored) {
        }
    }

    private String pickRandomAccount(long sizeBytes) throws UploadFailure {
        Iterable<TrustedAccount> snapshot = trustedAccountsRepo.getAccountsSnapshot();
        List<String> eligible = new ArrayList<>();
        for (TrustedAccount a : snapshot)
            if (a.totalQuota - a.usedQuota >= sizeBytes)
                eligible.add(a.email);
        if (eligible.isEmpty())
            throw new UploadFailure(FailureReason.NO_SPACE, "No account has space");
        return eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
    }

    private String uploadThumbnail(Drive drive, String path) throws Exception {
        String folderId = DriveFolderRepository.getThumbnailsRootId(appContext);
        java.io.File file = new java.io.File(path);

        File meta = new File()
                .setName("media_thumb_" + UUID.randomUUID().toString() + ".jpg")
                .setMimeType("image/jpeg")
                .setParents(Collections.singletonList(folderId));

        InputStreamContent content =
                new InputStreamContent("image/jpeg", new FileInputStream(file));
        content.setLength(file.length());

        return drive.files().create(meta, content)
                .setFields("id")
                .execute()
                .getId();
    }
}
