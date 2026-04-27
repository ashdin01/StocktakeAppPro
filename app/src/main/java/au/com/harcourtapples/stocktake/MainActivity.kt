package au.com.harcourtapples.stocktake

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.harcourtapples.stocktake.ui.detail.SessionDetailScreen
import au.com.harcourtapples.stocktake.ui.scan.ScanScreen
import au.com.harcourtapples.stocktake.ui.sessions.SessionsScreen
import au.com.harcourtapples.stocktake.ui.settings.SettingsScreen
import au.com.harcourtapples.stocktake.ui.theme.StocktakeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = (application as StocktakeApplication).settingsStore
        setContent {
            StocktakeTheme {
                val serverUrl by store.serverUrl.collectAsState(initial = SettingsStore.DEFAULT_URL)
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "sessions") {
                    composable("sessions") {
                        SessionsScreen(
                            serverUrl = serverUrl,
                            onOpenSession = { id -> navController.navigate("session/$id") },
                            onSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("session/{sessionId}") { back ->
                        val sessionId = back.arguments?.getString("sessionId")?.toIntOrNull() ?: return@composable
                        SessionDetailScreen(
                            sessionId = sessionId,
                            serverUrl = serverUrl,
                            onScan = { navController.navigate("scan/$sessionId") },
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("scan/{sessionId}") { back ->
                        val sessionId = back.arguments?.getString("sessionId")?.toIntOrNull() ?: return@composable
                        ScanScreen(
                            sessionId = sessionId,
                            serverUrl = serverUrl,
                            onBack = { navController.popBackStack() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            store = store,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
