package au.com.harcourtapples.stocktake.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import au.com.harcourtapples.stocktake.SettingsStore
import au.com.harcourtapples.stocktake.api.ApiClient
import au.com.harcourtapples.stocktake.api.TofuTrustManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    store: SettingsStore,
    onBack: () -> Unit,
    onSync: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val savedUrl         by store.serverUrl.collectAsState(initial = SettingsStore.DEFAULT_URL)
    val savedKey         by store.apiKey.collectAsState(initial = "")
    val offline          by store.offlineMode.collectAsState(initial = false)
    val savedFingerprint by store.trustedCertFingerprint.collectAsState(initial = "")

    var urlInput    by remember(savedUrl)  { mutableStateOf(savedUrl) }
    var keyInput    by remember(savedKey)  { mutableStateOf(savedKey) }
    var testStatus  by remember { mutableStateOf("") }
    var isTesting   by remember { mutableStateOf(false) }
    var saved       by remember { mutableStateOf(false) }

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
            // ── Mode toggle ────────────────────────────────────────────────

            Text("Connection Mode", style = MaterialTheme.typography.titleMedium)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeButton(
                    label = "WiFi",
                    description = "Live connection to BackOfficePro",
                    icon = { Icon(Icons.Default.Wifi, null, modifier = Modifier.size(20.dp)) },
                    selected = !offline,
                    modifier = Modifier.weight(1f),
                    onClick = { scope.launch { store.setOfflineMode(false) } }
                )
                ModeButton(
                    label = "Offline",
                    description = "Save locally, sync later",
                    icon = { Icon(Icons.Default.WifiOff, null, modifier = Modifier.size(20.dp)) },
                    selected = offline,
                    modifier = Modifier.weight(1f),
                    onClick = { scope.launch { store.setOfflineMode(true) } }
                )
            }

            if (offline) {
                OutlinedButton(
                    onClick = onSync,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Upload offline data to BackOfficePro")
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            // ── Server connection ──────────────────────────────────────────

            Text("Server Connection", style = MaterialTheme.typography.titleMedium)
            Text(
                "Enter the IP address of the PC running BackOfficePro.\n" +
                "Use https:// (default) for a secure connection. " +
                "Both devices must be on the same WiFi network.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it; saved = false },
                label = { Text("Server URL") },
                placeholder = { Text("https://192.168.1.100:5050") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it; saved = false },
                label = { Text("API Key") },
                placeholder = { Text("Paste from BackOfficePro Settings → API Access") },
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
                                // Configure trust manager for this test URL
                                val fpForTest = if (urlInput.trimEnd('/') == savedUrl.trimEnd('/'))
                                    savedFingerprint.ifBlank { null }
                                else
                                    null  // different URL — first-use mode
                                ApiClient.setTrustedFingerprint(fpForTest)

                                val resp = ApiClient.service(urlInput, keyInput).health()
                                if (resp.isSuccessful) {
                                    val seen = ApiClient.lastSeenFingerprint
                                    if (seen != null && seen != savedFingerprint) {
                                        store.setTrustedCertFingerprint(seen)
                                        testStatus = "Connected! Certificate trusted."
                                    } else {
                                        testStatus = "Connected!"
                                    }
                                } else {
                                    testStatus = "Server error ${resp.code()}"
                                }
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
                            val urlChanged = urlInput.trimEnd('/') != savedUrl.trimEnd('/')
                            store.setServerUrl(urlInput)
                            store.setApiKey(keyInput)
                            if (urlChanged) store.clearTrustedCertFingerprint()
                            ApiClient.invalidate()
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

            // ── Certificate trust ──────────────────────────────────────────

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            Text("Certificate Trust", style = MaterialTheme.typography.titleMedium)

            if (savedFingerprint.isBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.LockOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "Not yet trusted — tap Test to connect and trust the server certificate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Trusted certificate",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            savedFingerprint,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = { scope.launch { store.clearTrustedCertFingerprint() } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Forget Certificate")
                }

                Text(
                    "Tap \"Forget Certificate\" if the BackOfficePro server certificate " +
                    "was regenerated, then tap Test to re-trust it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 4.dp))

            Text("About", style = MaterialTheme.typography.titleMedium)
            Text(
                "BackOfficePro Stocktake Pro\nVersion 1.3.0\n\n" +
                "Scan EAN-13 and EAN-8 barcodes to record stock counts.\n" +
                "WiFi mode syncs to BackOfficePro in real time.\n" +
                "Offline mode saves locally — upload when back on WiFi.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ModeButton(
    label: String,
    description: String,
    icon: @Composable () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val containerColor = if (selected)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    val borderColor = if (selected)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outline

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            icon()
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
