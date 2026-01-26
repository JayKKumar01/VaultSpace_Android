package com.github.jaykkumar01.vaultspace.core.drive;

import com.github.jaykkumar01.vaultspace.models.TrustedAccount;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.About.StorageQuota;

public final class DriveStorageRepository {

    private DriveStorageRepository() {
    }

    public static TrustedAccount fetchStorageInfo(Drive drive, String email) throws Exception {

        StorageQuota storageQuota =
                drive.about()
                        .get()
                        .setFields("storageQuota(limit,usage)")
                        .execute().getStorageQuota();

        long limit = storageQuota.getLimit();
        long usage = storageQuota.getUsage();

        return new TrustedAccount(
                email,
                limit,
                usage,
                Math.max(0, limit - usage)
        );
    }
}
