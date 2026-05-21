package au.com.harcourtapples.stocktake

import android.app.Application
import au.com.harcourtapples.stocktake.db.StocktakeDatabase
import au.com.harcourtapples.stocktake.repository.StocktakeRepository

class StocktakeApplication : Application() {
    val settingsStore by lazy { SettingsStore(applicationContext) }
    val database by lazy { StocktakeDatabase.get(applicationContext) }
    val repository by lazy { StocktakeRepository(database.sessionDao(), database.countDao()) }
}
