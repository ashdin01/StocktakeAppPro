package au.com.harcourtapples.stocktake.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.harcourtapples.stocktake.api.ApiClient
import au.com.harcourtapples.stocktake.api.models.AddCountRequest
import au.com.harcourtapples.stocktake.api.models.Product
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ScanState {
    object Scanning : ScanState()
    object LookingUp : ScanState()
    data class ProductFound(val product: Product, val barcode: String) : ScanState()
    data class NotFound(val barcode: String) : ScanState()
    object Saving : ScanState()
    data class Error(val message: String) : ScanState()
}

class ScanViewModel : ViewModel() {

    private val _state = MutableStateFlow<ScanState>(ScanState.Scanning)
    val state: StateFlow<ScanState> = _state

    private val _debug = MutableStateFlow("camera starting...")
    val debug: StateFlow<String> = _debug

    fun updateDebug(info: String) { _debug.value = info }

    private var lastBarcode: String? = null

    fun onBarcodeDetected(barcode: String, serverUrl: String) {
        val current = _state.value
        if (current !is ScanState.Scanning) return
        if (barcode == lastBarcode) return
        lastBarcode = barcode
        _state.value = ScanState.LookingUp

        viewModelScope.launch {
            try {
                val resp = ApiClient.service(serverUrl).getProduct(barcode)
                _state.value = when {
                    resp.isSuccessful -> ScanState.ProductFound(resp.body()!!, barcode)
                    resp.code() == 404 -> ScanState.NotFound(barcode)
                    else -> ScanState.Error("Server error ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = ScanState.Error(e.message ?: "Network error")
            }
        }
    }

    fun submitCount(sessionId: Int, barcode: String, qty: Double, serverUrl: String) {
        _state.value = ScanState.Saving
        viewModelScope.launch {
            try {
                val resp = ApiClient.service(serverUrl)
                    .addCount(sessionId, AddCountRequest(barcode, qty))
                if (resp.isSuccessful) {
                    delay(400)
                    reset()
                } else {
                    _state.value = ScanState.Error("Failed to save: ${resp.code()}")
                }
            } catch (e: Exception) {
                _state.value = ScanState.Error(e.message ?: "Network error")
            }
        }
    }

    fun reset() {
        lastBarcode = null
        _state.value = ScanState.Scanning
    }
}
