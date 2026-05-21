package au.com.harcourtapples.stocktake.db.dao

import androidx.room.*
import au.com.harcourtapples.stocktake.db.entities.LocalSession

@Dao
interface LocalSessionDao {
    @Query("SELECT * FROM local_sessions ORDER BY startedAt DESC")
    suspend fun getAll(): List<LocalSession>

    @Query("SELECT * FROM local_sessions WHERE id = :id")
    suspend fun getById(id: Int): LocalSession?

    @Query("SELECT * FROM local_sessions WHERE synced = 0 ORDER BY startedAt ASC")
    suspend fun getUnsynced(): List<LocalSession>

    @Insert
    suspend fun insert(session: LocalSession): Long

    @Update
    suspend fun update(session: LocalSession)

    @Delete
    suspend fun delete(session: LocalSession)
}
