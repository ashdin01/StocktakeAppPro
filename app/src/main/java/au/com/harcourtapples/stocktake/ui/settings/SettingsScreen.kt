package au.com.harcourtapples.stocktake.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import au.com.harcourtapples.stocktake.SettingsStore
import au.com.harcourtapples.stocktake.api.ApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: SettingsStore,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val savedUrl by store.serverUrl.collectAsState(initial = SettingsStore.DEFAULT_URL)
    var urlInput by remember(savedUrl) { mutableStateOf(savedUrl) }
    var testStatus by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Server Connection", style = MaterialTheme.typography.titleMedium)
            Text(
                "Enter the IP address of the PC running BackOfficePro.\n" +
                "Both devices must be on the same WiFi network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it; saved = false },
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.100:5050") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        isTesting = true
                        testStatus = ""
                        scope.launch {
                            try {
                                val resp = ApiClient.service(urlInput).health()
                                testStatus = if (resp.isSuccessful) "Connected!" else "Server error ${resp.code()}"
                            } catch (e: Exception) {
                                testStatus = "Cannot connect: ${e.message}"
                            } finally {
                                isTesting = false
                            }
                        }
                    },
                    enabled = !isTesting
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Test")
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            store.setServerUrl(urlInput)
                            saved = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Save")
                }
            }

            if (testStatus.isNotEmpty()) {
                Text(
                    testStatus,
                    color = if (testStatus.startsWith("Connected"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (saved) {
                Text(
                    "Saved",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "BackOfficePro Stocktake\nVersion 1.0.0\n\n" +
                "Scan EAN-13 and EAN-8 barcodes to record stock counts.\n" +
                "Data syncs to BackOfficePro running on your local network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
