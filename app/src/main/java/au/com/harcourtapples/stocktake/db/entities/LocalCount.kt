package au.com.harcourtapples.stocktake.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_counts",
    foreignKeys = [
        ForeignKey(
            entity = LocalSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class LocalCount(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val barcode: String,
    val description: String,
    val qty: Double,
    val scannedAt: String,
    val synced: Boolean = false
)
