package au.com.harcourtapples.stocktake.db.dao

import androidx.room.*
import au.com.harcourtapples.stocktake.db.entities.LocalCount

@Dao
interface LocalCountDao {
    @Query("SELECT * FROM local_counts WHERE sessionId = :sessionId ORDER BY scannedAt DESC")
    suspend fun getBySession(sessionId: Int): List<LocalCount>

    @Query("SELECT * FROM local_counts WHERE sessionId = :sessionId AND synced = 0")
    suspend fun getUnsyncedForSession(sessionId: Int): List<LocalCount>

    @Query("SELECT COUNT(*) FROM local_counts WHERE sessionId = :sessionId")
    suspend fun countForSession(sessionId: Int): Int

    @Query("SELECT * FROM local_counts WHERE sessionId = :sessionId AND barcode = :barcode LIMIT 1")
    suspend fun getBySessionAndBarcode(sessionId: Int, barcode: String): LocalCount?

    @Insert
    suspend fun insert(count: LocalCount): Long

    @Update
    suspend fun update(count: LocalCount)

    @Query("DELETE FROM local_counts WHERE id = :id")
    suspend fun deleteById(id: Int)
}
