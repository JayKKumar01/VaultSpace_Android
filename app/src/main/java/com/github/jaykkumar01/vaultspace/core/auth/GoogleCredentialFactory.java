package com.github.jaykkumar01.vaultspace.core.auth;

import android.content.Context;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

public final class GoogleCredentialFactory {

    private GoogleCredentialFactory() {}

    /* ---------------- Profile only ---------------- */

    public static GoogleAccountCredential forProfile(Context context, String email) {
        return create(
                context,
                email,
                Collections.singleton("https://www.googleapis.com/auth/userinfo.profile")
        );
    }

    /* ---------------- Drive only ---------------- */

    public static GoogleAccountCredential forDrive(Context context, String email) {
        return create(
                context,
                email,
                Collections.singleton("https://www.googleapis.com/auth/drive.file")
        );
    }

    /* ---------------- Primary Account (Drive + Profile) ---------------- */

    public static GoogleAccountCredential forPrimaryAccount(Context context, String email) {
        return create(
                context,
                email,
                Arrays.asList(
                        "https://www.googleapis.com/auth/drive.file",
                        "https://www.googleapis.com/auth/userinfo.profile"
                )
        );
    }

    /* ---------------- Core ---------------- */

    private static GoogleAccountCredential create(
            Context context,
            String email,
            Collection<String> scopes
    ) {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(context.getApplicationContext(), scopes);
        credential.setSelectedAccountName(email);
        return credential;
    }
}
