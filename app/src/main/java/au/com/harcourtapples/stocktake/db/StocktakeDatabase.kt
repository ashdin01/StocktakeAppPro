package au.com.harcourtapples.stocktake.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import au.com.harcourtapples.stocktake.db.dao.LocalCountDao
import au.com.harcourtapples.stocktake.db.dao.LocalSessionDao
import au.com.harcourtapples.stocktake.db.entities.LocalCount
import au.com.harcourtapples.stocktake.db.entities.LocalSession

@Database(entities = [LocalSession::class, LocalCount::class], version = 1, exportSchema = false)
abstract class StocktakeDatabase : RoomDatabase() {
    abstract fun sessionDao(): LocalSessionDao
    abstract fun countDao(): LocalCountDao

    companion object {
        @Volatile private var INSTANCE: StocktakeDatabase? = null

        fun get(context: Context): StocktakeDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    StocktakeDatabase::class.java,
                    "stocktake_offline.db"
                ).build().also { INSTANCE = it }
            }
    }
}
