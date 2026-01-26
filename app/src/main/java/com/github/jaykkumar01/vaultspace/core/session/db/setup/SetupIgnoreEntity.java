package com.github.jaykkumar01.vaultspace.core.session.db.setup;

import androidx.annotation.NonNull;
import androidx.room.Entity;

@Entity(
        tableName = "setup_ignore",
        primaryKeys = { "email" }
)
public final class SetupIgnoreEntity {

    @NonNull public final String email;
    public final long ignoredAtMillis;

    public SetupIgnoreEntity(
            @NonNull String email,
            long ignoredAtMillis
    ) {
        this.email = email;
        this.ignoredAtMillis = ignoredAtMillis;
    }
}
