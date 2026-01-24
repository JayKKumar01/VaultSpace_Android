package com.github.jaykkumar01.vaultspace.core.session.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface UploadRetryDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insert(UploadRetryEntity entity);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<UploadRetryEntity> entities);

    @Query("""
            SELECT EXISTS(
                SELECT 1 FROM upload_retry
                WHERE id = :id
            )
           """)
    boolean containsId(String id);

    @Query("""
        UPDATE upload_retry
        SET failureReason = :reason
        WHERE id = :id
       """)
    void updateFailureReason(String id, String reason);


    @Query("""
            DELETE FROM upload_retry
            WHERE id = :id
           """)
    void deleteById(String id);

    @Query("""
            DELETE FROM upload_retry
            WHERE groupId = :groupId
           """)
    void deleteGroup(String groupId);

    @Query("DELETE FROM upload_retry")
    void deleteAll();

    @Query("""
            SELECT * FROM upload_retry
            WHERE groupId = :groupId
           """)
    List<UploadRetryEntity> getByGroup(String groupId);

    @Query("SELECT * FROM upload_retry")
    List<UploadRetryEntity> getAll();
}
