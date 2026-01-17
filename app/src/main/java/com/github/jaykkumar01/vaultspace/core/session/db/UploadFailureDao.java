package com.github.jaykkumar01.vaultspace.core.session.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UploadFailureDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UploadFailureEntity entity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UploadFailureEntity> entities);

    @Query("SELECT * FROM upload_failure WHERE groupId = :groupId")
    List<UploadFailureEntity> getByGroup(String groupId);

    /* 🔑 delete exact item (success / retry cleanup) */
    @Query("""
        DELETE FROM upload_failure
        WHERE groupId = :groupId
          AND uri = :uri
          AND type = :type
    """)
    void delete(
            String groupId,
            String uri,
            String type
    );

    /* optional: bulk cleanup when retrying subset */
    @Query("""
        DELETE FROM upload_failure
        WHERE groupId = :groupId
          AND uri IN (:uris)
    """)
    void deleteByUris(
            String groupId,
            List<String> uris
    );

    @Query("DELETE FROM upload_failure WHERE groupId = :groupId")
    void deleteByGroup(String groupId);

    @Query("DELETE FROM upload_failure")
    void deleteAll();
}
