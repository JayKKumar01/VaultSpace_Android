package com.github.jaykkumar01.vaultspace.dashboard.albums.helper;

import android.content.Context;

import androidx.annotation.Nullable;

import com.github.jaykkumar01.vaultspace.core.drive.DriveClientProvider;
import com.google.api.services.drive.Drive;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public final class CoverResolver {

    private static final String DIR_NAME = "album_covers";

    private final Drive drive;
    private final File coversDir;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public CoverResolver(Context context, Drive drive) {
        Context app = context.getApplicationContext();
        this.drive = drive;
        coversDir = new File(app.getCacheDir(), DIR_NAME);
        if (!coversDir.exists()) coversDir.mkdirs();
    }

    @Nullable
    public String resolve(@Nullable String coverFileId) throws Exception {
        if (coverFileId == null) return null;

        File out = new File(coversDir, coverFileId);
        if (out.exists()) return out.getAbsolutePath();

        try (OutputStream os = new FileOutputStream(out)) {
            drive.files().get(coverFileId).executeMediaAndDownloadTo(os);
        }

        return out.getAbsolutePath();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void clear(@Nullable String coverFileId) {
        if (coverFileId == null) return;
        File f = new File(coversDir, coverFileId);
        if (f.exists()) f.delete();
    }
}
