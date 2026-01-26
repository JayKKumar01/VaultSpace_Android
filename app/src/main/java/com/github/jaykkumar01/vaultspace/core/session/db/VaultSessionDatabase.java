package com.github.jaykkumar01.vaultspace.core.session.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.github.jaykkumar01.vaultspace.core.session.db.retry.UploadRetryDao;
import com.github.jaykkumar01.vaultspace.core.session.db.retry.UploadRetryEntity;
import com.github.jaykkumar01.vaultspace.core.session.db.setup.SetupIgnoreDao;
import com.github.jaykkumar01.vaultspace.core.session.db.setup.SetupIgnoreEntity;

@Database(
        entities = {
                UploadRetryEntity.class,
                SetupIgnoreEntity.class
        },
        version = 1,
        exportSchema = false
)
public abstract class VaultSessionDatabase extends RoomDatabase {

    private static volatile VaultSessionDatabase INSTANCE;

    public abstract UploadRetryDao uploadRetryDao();
    public abstract SetupIgnoreDao setupIgnoreDao();

    public static VaultSessionDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (VaultSessionDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            VaultSessionDatabase.class,
                            "vault_session.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
