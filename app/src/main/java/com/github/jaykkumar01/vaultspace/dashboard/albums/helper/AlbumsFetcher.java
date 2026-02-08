package com.github.jaykkumar01.vaultspace.dashboard.albums.helper;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.util.ArrayList;
import java.util.List;

public final class AlbumsFetcher {

    private final Drive drive;

    AlbumsFetcher(Drive drive) {
        this.drive = drive;
    }

    public List<File> fetchAll(String albumsRootId) throws Exception {
        List<File> out = new ArrayList<>();
        String pageToken = null;

        do {
            FileList list = drive.files().list()
                    .setQ("'" + albumsRootId + "' in parents and mimeType='application/vnd.google-apps.folder' and trashed=false")
                    .setOrderBy("modifiedTime desc")
                    .setFields("nextPageToken,files(id,name,createdTime,modifiedTime,appProperties)")
                    .setPageToken(pageToken)
                    .execute();

            if (list.getFiles() != null) out.addAll(list.getFiles());
            pageToken = list.getNextPageToken();

        } while (pageToken != null);

        return out;
    }
}
