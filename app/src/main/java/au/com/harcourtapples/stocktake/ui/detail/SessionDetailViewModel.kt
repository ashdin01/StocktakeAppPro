package au.com.harcourtapples.stocktake.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.harcourtapples.stocktake.api.ApiClient
import au.com.harcourtapples.stocktake.api.models.Count
import au.com.harcourtapples.stocktake.api.models.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class DetailUiState(
    val session: Session? = null,
    val counts: List<Count> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SessionDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state

    fun load(serverUrl: String, sessionId: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.service(serverUrl)
                val sessionResp = api.getSession(sessionId)
                val countsResp  = api.getCounts(sessionId)
                if (sessionResp.isSuccessful && countsResp.isSuccessful) {
                    _state.value = _state.value.copy(
                        session  = sessionResp.body(),
                        counts   = countsResp.body() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Server error ${sessionResp.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun deleteCount(serverUrl: String, sessionId: Int, countId: Int) {
        viewModelScope.launch {
            try {
                ApiClient.service(serverUrl).deleteCount(sessionId, countId)
                _state.value = _state.value.copy(
                    counts = _state.value.counts.filter { it.id != countId }
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }
}
