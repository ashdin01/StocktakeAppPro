package au.com.harcourtapples.stocktake

import android.app.Application

class StocktakeApplication : Application() {
    val settingsStore by lazy { SettingsStore(applicationContext) }
}
