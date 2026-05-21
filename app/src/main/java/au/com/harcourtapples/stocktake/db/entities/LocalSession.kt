package au.com.harcourtapples.stocktake.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_sessions")
data class LocalSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String,
    val notes: String = "",
    val startedAt: String,
    val synced: Boolean = false,
    val serverId: Int? = null
)
