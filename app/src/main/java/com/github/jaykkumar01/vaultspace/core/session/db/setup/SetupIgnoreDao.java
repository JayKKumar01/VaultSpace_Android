package com.github.jaykkumar01.vaultspace.core.session.db.setup;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SetupIgnoreDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SetupIgnoreEntity e);

    @Query("DELETE FROM setup_ignore WHERE email = :email")
    void delete(String email);

    @Query("DELETE FROM setup_ignore")
    void clear();

    @Query("SELECT email FROM setup_ignore")
    List<String> getAllIgnoredEmails();
}
