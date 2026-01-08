package com.github.jaykkumar01.vaultspace.core.drive;

import android.content.Context;

import com.github.jaykkumar01.vaultspace.core.auth.GoogleCredentialFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;

public final class DriveClientProvider {

    private static final String APP_NAME = "VaultSpace";

    private DriveClientProvider() {}

    public static Drive forAccount(Context context, String email) {
        GoogleAccountCredential credential =
                GoogleCredentialFactory.forDrive(context, email);

        return new Drive.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        )
                .setApplicationName(APP_NAME)
                .build();
    }
}
