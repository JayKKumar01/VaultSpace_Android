package com.github.jaykkumar01.vaultspace.core.drive;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;

public final class DrivePermissionRepository {

    private DrivePermissionRepository() {}

    public static boolean hasWriterAccess(
            Drive drive,
            String folderId,
            String email
    ) throws Exception {

        PermissionList permissions =
                drive.permissions()
                        .list(folderId)
                        .setFields("permissions(role,emailAddress)")
                        .execute();

        for (Permission p : permissions.getPermissions()) {
            if ("writer".equals(p.getRole())
                    && email.equalsIgnoreCase(p.getEmailAddress())) {
                return true;
            }
        }
        return false;
    }
}
