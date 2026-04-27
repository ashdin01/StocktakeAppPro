package au.com.harcourtapples.stocktake.ui.sessions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.harcourtapples.stocktake.api.ApiClient
import au.com.harcourtapples.stocktake.api.models.CreateSessionRequest
import au.com.harcourtapples.stocktake.api.models.Department
import au.com.harcourtapples.stocktake.api.models.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SessionsUiState(
    val sessions: List<Session> = emptyList(),
    val departments: List<Department> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SessionsViewModel : ViewModel() {

    private val _state = MutableStateFlow(SessionsUiState())
    val state: StateFlow<SessionsUiState> = _state

    fun load(serverUrl: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val api = ApiClient.service(serverUrl)
                val sessions = api.getSessions()
                val depts = api.getDepartments()
                if (sessions.isSuccessful && depts.isSuccessful) {
                    _state.value = _state.value.copy(
                        sessions = sessions.body() ?: emptyList(),
                        departments = depts.body() ?: emptyList(),
                        isLoading = false
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Server returned ${sessions.code()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Cannot connect: ${e.message}"
                )
            }
        }
    }

    fun createSession(
        serverUrl: String,
        label: String,
        deptId: Int?,
        onSuccess: (Int) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val resp = ApiClient.service(serverUrl)
                    .createSession(CreateSessionRequest(label, deptId))
                if (resp.isSuccessful) {
                    val id = resp.body()?.get("id") ?: return@launch
                    onSuccess(id)
                    load(serverUrl)
                } else {
                    _state.value = _state.value.copy(error = "Failed to create session")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e.message)
            }
        }
    }

    fun dismissError() {
        _state.value = _state.value.copy(error = null)
    }
}
