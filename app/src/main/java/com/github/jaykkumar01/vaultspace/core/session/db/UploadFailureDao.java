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

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM upload_failure
            WHERE groupId = :groupId
              AND uri = :uri
              AND type = :type
        )
    """)
    boolean contains(String groupId,String uri,String type);

    @Query("""
    SELECT * FROM upload_failure
    WHERE uri = :uri
    LIMIT 1
""")
    UploadFailureEntity getByUri(String uri);


    @Query("""
        DELETE FROM upload_failure
        WHERE groupId = :groupId
          AND uri = :uri
          AND type = :type
    """)
    void delete(String groupId,String uri,String type);

    @Query("""
        DELETE FROM upload_failure
        WHERE groupId = :groupId
          AND uri IN (:uris)
    """)
    void deleteByUris(String groupId,List<String> uris);

    @Query("DELETE FROM upload_failure WHERE groupId = :groupId")
    void deleteByGroup(String groupId);

    @Query("DELETE FROM upload_failure")
    void deleteAll();
}
