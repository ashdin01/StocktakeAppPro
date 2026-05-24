package au.com.harcourtapples.stocktake.ui.scan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import au.com.harcourtapples.stocktake.StocktakeApplication
import au.com.harcourtapples.stocktake.scanner.BarcodeAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    sessionId: Int,
    serverUrl: String,
    apiKey: String = "",
    offline: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as StocktakeApplication
    val vm: ScanViewModel = viewModel(factory = ScanViewModel.factory(app.repository))
    val state by vm.state.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Barcode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (!hasCameraPermission) {
                Column(
                    Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera permission required", color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Grant Permission")
                    }
                }
            } else {
                CameraPreview(
                    isAnalyzing = state is ScanState.Scanning,
                    onBarcodeDetected = { barcode -> vm.onBarcodeDetected(barcode, sessionId, serverUrl, offline, apiKey) },
                )

                ScanOverlay(Modifier.fillMaxSize())

                when (val s = state) {
                    is ScanState.Scanning -> Text(
                        "Point at an EAN-13 or EAN-8 barcode",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    is ScanState.LookingUp -> CircularProgressIndicator(
                        Modifier.align(Alignment.Center),
                        color = Color.White
                    )

                    is ScanState.ProductFound -> ProductBottomSheet(
                        product = s.product,
                        barcode = s.barcode,
                        existingQty = s.existingQty,
                        onConfirm = { newTotal -> vm.submitCount(sessionId, s.barcode, newTotal - s.existingQty, serverUrl, offline, apiKey = apiKey) },
                        onCancel = { vm.reset() }
                    )

                    is ScanState.NotFound -> NotFoundSheet(
                        barcode = s.barcode,
                        onDismiss = { vm.reset() }
                    )

                    is ScanState.OfflineReady -> OfflineCountSheet(
                        barcode = s.barcode,
                        existingQty = s.existingQty,
                        onConfirm = { desc, newTotal -> vm.submitCount(sessionId, s.barcode, newTotal - s.existingQty, serverUrl, offline, desc, apiKey) },
                        onCancel = { vm.reset() }
                    )

                    is ScanState.Saving -> CircularProgressIndicator(
                        Modifier.align(Alignment.Center),
                        color = Color.White
                    )

                    is ScanState.Error -> ErrorSheet(
                        message = s.message,
                        onDismiss = { vm.reset() }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(isAnalyzing: Boolean, onBarcodeDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    val callbackRef = remember { mutableStateOf(onBarcodeDetected) }
    callbackRef.value = onBarcodeDetected

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analyzer = ImageAnalysis.Builder()
                    .setTargetResolution(android.util.Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, BarcodeAnalyzer { barcode ->
                            callbackRef.value(barcode)
                        })
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analyzer
                    )
                } catch (_: Exception) {}
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScanOverlay(modifier: Modifier) {
    Box(modifier) {
        Box(
            Modifier
                .size(260.dp)
                .align(Alignment.Center)
                .border(2.dp, Color.White, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun ProductBottomSheet(
    product: au.com.harcourtapples.stocktake.api.models.Product,
    barcode: String,
    existingQty: Double = 0.0,
    onConfirm: (Double) -> Unit,
    onCancel: () -> Unit
) {
    val initialQty = if (existingQty > 0)
        existingQty.let { if (it % 1 == 0.0) it.toInt().toString() else String.format("%.2f", it) }
    else "1"
    var qtyText by remember { mutableStateOf(initialQty) }
    val isUpdate = existingQty > 0

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(product.description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${product.deptName ?: ""}  •  ${product.brand ?: ""}  •  SOH: ${product.sohQty}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(barcode, style = MaterialTheme.typography.bodySmall)
                QtyRow(
                    qtyText = qtyText,
                    onQtyChange = { qtyText = it },
                    label = if (isUpdate) "Counted quantity (adjust up or down)" else "Quantity"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(qtyText.toDoubleOrNull() ?: existingQty) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isUpdate) "Update Count" else "Add to Session") }
                }
            }
        }
    }
}

@Composable
private fun OfflineCountSheet(
    barcode: String,
    existingQty: Double = 0.0,
    onConfirm: (String, Double) -> Unit,
    onCancel: () -> Unit
) {
    val initialQty = if (existingQty > 0)
        existingQty.let { if (it % 1 == 0.0) it.toInt().toString() else String.format("%.2f", it) }
    else "1"
    var description by remember { mutableStateOf("") }
    var qtyText by remember { mutableStateOf(initialQty) }
    val isUpdate = existingQty > 0

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Offline Count", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    barcode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Leave blank to use barcode") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                QtyRow(
                    qtyText = qtyText,
                    onQtyChange = { qtyText = it },
                    label = if (isUpdate) "Counted quantity (adjust up or down)" else "Quantity"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(description, qtyText.toDoubleOrNull() ?: existingQty) },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (isUpdate) "Update Count" else "Save Offline") }
                }
            }
        }
    }
}

@Composable
private fun QtyRow(qtyText: String, onQtyChange: (String) -> Unit, label: String = "Quantity") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = { onQtyChange(((qtyText.toDoubleOrNull() ?: 1.0) - 1).let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() }) }
        ) { Text("−", style = MaterialTheme.typography.headlineSmall) }

        OutlinedTextField(
            value = qtyText,
            onValueChange = onQtyChange,
            label = { Text(label) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            modifier = Modifier.weight(1f)
        )

        IconButton(
            onClick = { onQtyChange(((qtyText.toDoubleOrNull() ?: 0.0) + 1).let { if (it % 1 == 0.0) it.toInt().toString() else it.toString() }) }
        ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
    }
}

@Composable
private fun NotFoundSheet(barcode: String, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Barcode not found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(barcode, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                Button(onClick = onDismiss) { Text("Scan Next") }
            }
        }
    }
}

@Composable
private fun ErrorSheet(message: String, onDismiss: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Error", style = MaterialTheme.typography.titleMedium)
                Text(message, textAlign = TextAlign.Center)
                Button(onClick = onDismiss) { Text("OK") }
            }
        }
    }
}
