package au.com.harcourtapples.stocktake.ui.sessions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.harcourtapples.stocktake.StocktakeApplication
import au.com.harcourtapples.stocktake.api.models.Department
import au.com.harcourtapples.stocktake.api.models.DeptGroup
import au.com.harcourtapples.stocktake.api.models.Session

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    serverUrl: String,
    apiKey: String = "",
    offline: Boolean,
    onOpenSession: (Int) -> Unit,
    onSettings: () -> Unit,
    onSync: () -> Unit
) {
    val app = LocalContext.current.applicationContext as StocktakeApplication
    val vm: SessionsViewModel = viewModel(factory = SessionsViewModel.factory(app.repository))
    val state by vm.state.collectAsState()
    var showNewDialog by remember { mutableStateOf(false) }

    LaunchedEffect(serverUrl, apiKey, offline) { vm.load(serverUrl, offline, apiKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Stocktake Sessions")
                        if (offline) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = "Offline mode",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (offline) {
                        IconButton(onClick = onSync) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Sync")
                        }
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Session")
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> ErrorMessage(
                    message = state.error!!,
                    onRetry = { vm.load(serverUrl, offline, apiKey) }
                )
                state.sessions.isEmpty() -> Text(
                    if (offline) "No offline sessions yet.\nTap + to create one."
                    else "No sessions yet. Tap + to create one.",
                    modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.sessions, key = { it.id }) { session ->
                        SessionCard(session = session, onClick = { onOpenSession(session.id) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        NewSessionDialog(
            departments = if (offline) emptyList() else state.departments,
            groups = state.groups,
            offline = offline,
            onDismiss = { showNewDialog = false },
            onDeptSelected = { deptId -> vm.loadGroupsForDept(serverUrl, deptId, apiKey) },
            onCreate = { label, deptId, groupId ->
                showNewDialog = false
                vm.createSession(serverUrl, offline, label, deptId, groupId, apiKey = apiKey, onSuccess = onOpenSession)
            }
        )
    }
}

@Composable
private fun SessionCard(session: Session, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(session.label, fontWeight = FontWeight.SemiBold) },
        supportingContent = {
            Column {
                val scope = when {
                    session.deptName != null && session.groupName != null ->
                        "${session.deptName} > ${session.groupName}"
                    session.deptName != null -> session.deptName
                    else -> null
                }
                scope?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                Text(
                    "${session.lineCount} items  •  ${session.startedAt.take(10)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingContent = {
            Badge(
                containerColor = if (session.status == "OPEN")
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(session.status)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewSessionDialog(
    departments: List<Department>,
    groups: List<DeptGroup>,
    offline: Boolean,
    onDismiss: () -> Unit,
    onDeptSelected: (Int?) -> Unit,
    onCreate: (String, Int?, Int?) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf<Department?>(null) }
    var selectedGroup by remember { mutableStateOf<DeptGroup?>(null) }
    var deptExpanded by remember { mutableStateOf(false) }
    var groupExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Stocktake Session") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Session Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!offline && departments.isNotEmpty()) {
                    ExposedDropdownMenuBox(expanded = deptExpanded, onExpandedChange = { deptExpanded = it }) {
                        OutlinedTextField(
                            value = selectedDept?.name ?: "All Departments",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Department (optional)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(deptExpanded) },
                            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = deptExpanded, onDismissRequest = { deptExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("All Departments") },
                                onClick = {
                                    selectedDept = null
                                    selectedGroup = null
                                    deptExpanded = false
                                    onDeptSelected(null)
                                }
                            )
                            departments.forEach { dept ->
                                DropdownMenuItem(
                                    text = { Text(dept.name) },
                                    onClick = {
                                        selectedDept = dept
                                        selectedGroup = null
                                        deptExpanded = false
                                        onDeptSelected(dept.id)
                                    }
                                )
                            }
                        }
                    }

                    if (selectedDept != null && groups.isNotEmpty()) {
                        ExposedDropdownMenuBox(expanded = groupExpanded, onExpandedChange = { groupExpanded = it }) {
                            OutlinedTextField(
                                value = selectedGroup?.name ?: "All Groups",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Group (optional)") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupExpanded) },
                                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = groupExpanded, onDismissRequest = { groupExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text("All Groups") },
                                    onClick = { selectedGroup = null; groupExpanded = false }
                                )
                                groups.forEach { group ->
                                    DropdownMenuItem(
                                        text = { Text(group.name) },
                                        onClick = { selectedGroup = group; groupExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (label.isNotBlank()) onCreate(label.trim(), selectedDept?.id, selectedGroup?.id) },
                enabled = label.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ErrorMessage(message: String, onRetry: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}
