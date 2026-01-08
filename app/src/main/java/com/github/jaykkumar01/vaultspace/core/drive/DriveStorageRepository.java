package com.github.jaykkumar01.vaultspace.core.drive;

import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About;

public final class DriveStorageRepository {

    private DriveStorageRepository() {}

    public static TrustedAccount fetchStorageInfo(
            Drive drive,
            String email
    ) throws Exception {

        About about =
                drive.about()
                        .get()
                        .setFields("storageQuota(limit,usage)")
                        .execute();

        long limit = about.getStorageQuota().getLimit();
        long usage = about.getStorageQuota().getUsage();

        return new TrustedAccount(
                email,
                limit,
                usage,
                Math.max(0, limit - usage)
        );
    }
}
