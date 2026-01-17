package com.github.jaykkumar01.vaultspace.core.session.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = { UploadFailureEntity.class },
        version = 1,
        exportSchema = false
)
public abstract class UploadFailureDatabase extends RoomDatabase {

    private static volatile UploadFailureDatabase INSTANCE;

    public abstract UploadFailureDao dao();

    public static UploadFailureDatabase get(Context context){
        if (INSTANCE == null) {
            synchronized (UploadFailureDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            UploadFailureDatabase.class,
                            "upload_failure.db"
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
