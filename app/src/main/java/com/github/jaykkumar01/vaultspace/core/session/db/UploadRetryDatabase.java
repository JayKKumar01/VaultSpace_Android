package com.github.jaykkumar01.vaultspace.core.session.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = { UploadRetryEntity.class },
        version = 1,
        exportSchema = false
)
public abstract class UploadRetryDatabase extends RoomDatabase {

    private static volatile UploadRetryDatabase INSTANCE;

    public abstract UploadRetryDao dao();

    public static UploadRetryDatabase get(Context context){
        if (INSTANCE == null) {
            synchronized (UploadRetryDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            UploadRetryDatabase.class,
                            "upload_retry.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
