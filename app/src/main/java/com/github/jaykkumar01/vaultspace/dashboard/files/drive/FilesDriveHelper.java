package com.github.jaykkumar01.vaultspace.dashboard.files.drive;

import android.content.Context;
import androidx.annotation.NonNull;
import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.github.jaykkumar01.vaultspace.core.drive.DriveFolderRepository;
import com.github.jaykkumar01.vaultspace.models.FileNode;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public final class FilesDriveHelper {

    /* ================= Constants ================= */

    private static final String FOLDER_MIME = "application/vnd.google-apps.folder";
    private static final String BASE_FIELDS = "nextPageToken,files(id,name,mimeType,size,modifiedTime,parents)";
    private static final int PAGE_SIZE = 1000;

    /* ================= Callbacks ================= */

    public interface Success { void call(List<FileNode> nodes); }
    public interface Failure { void call(Exception e); }

    /* ================= State ================= */

    private final Context context;

    public FilesDriveHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    /* ================= Root ================= */

    public String resolveFilesRoot() throws Exception {
        return DriveFolderRepository.getFilesRootId(context);
    }

    /* ================= Public API (Per Folder Load) ================= */

    public void fetchFolderChildren(Executor executor,
                                    @NonNull String parentId,
                                    @NonNull Success ok,
                                    @NonNull Failure err) {
        executor.execute(() -> {
            try {
                Drive drive = DriveClientProvider.getPrimaryDrive(context);
                List<FileNode> result = fetchChildrenOf(drive, parentId);
                ok.call(result);
            } catch (Exception e) {
                err.call(e);
            }
        });
    }

    /* ================= Internal ================= */

    private List<FileNode> fetchChildrenOf(Drive drive, String parentId) throws Exception {
        List<FileNode> collector = new ArrayList<>();
        String pageToken = null;

        do {
            FileList list = drive.files().list()
                    .setQ("'" + parentId + "' in parents and trashed=false")
                    .setFields(BASE_FIELDS)
                    .setOrderBy("folder,name")
                    .setPageSize(PAGE_SIZE)
                    .setPageToken(pageToken)
                    .execute();

            if (list.getFiles() != null) {
                for (File f : list.getFiles())
                    collector.add(toNode(f));
            }

            pageToken = list.getNextPageToken();
        } while (pageToken != null);

        return collector;
    }

    private FileNode toNode(File f) {
        long size = f.getSize() == null ? 0L : f.getSize();
        long modified = f.getModifiedTime() == null ? 0L : f.getModifiedTime().getValue();
        return new FileNode(f.getId(), f.getName(), f.getMimeType(), size, modified);
    }

    /* ================= Utility ================= */

    public boolean isFolder(FileNode node) {
        return FOLDER_MIME.equals(node.mimeType);
    }
}
