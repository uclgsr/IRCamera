package mpdc4gsr.viewmodel

import androidx.lifecycle.viewModelScope
import com.mpdc4gsr.libunified.app.http.repository.LmsRepository
import com.mpdc4gsr.libunified.app.ktbase.BaseViewModel
import com.mpdc4gsr.libunified.app.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Modernized PolicyViewModel using StateFlow and Repository pattern
 * Demonstrates modern MVVM architecture with proper state management
 */
class PolicyViewModel : BaseViewModel() {

    // State management with StateFlow
    private val _policyState = MutableStateFlow<PolicyState>(PolicyState.Idle)
    val policyState: StateFlow<PolicyState> = _policyState.asStateFlow()

    // One-time events with SharedFlow
    private val _events = MutableSharedFlow<PolicyEvent>()
    val events: SharedFlow<PolicyEvent> = _events.asSharedFlow()

    // Repository for data operations
    private val policyRepository = PolicyRepository()

    // Sealed classes for type-safe state management
    sealed class PolicyState {
        object Idle : PolicyState()
        object Loading : PolicyState()
        data class Success(val htmlData: HtmlBean) : PolicyState()
        data class Error(val message: String) : PolicyState()
    }

    sealed class PolicyEvent {
        data class ShowError(val message: String) : PolicyEvent()
        data class NavigateToUrl(val url: String) : PolicyEvent()
        object ShowNetworkError : PolicyEvent()
    }

    data class HtmlBean(
        val body: String? = null,
        val action: Int = 0,
        val title: String? = null,
        val type: PolicyType = PolicyType.PRIVACY
    )

    enum class PolicyType(val value: Int) {
        PRIVACY(1),
        TERMS(2),
        ABOUT(3);

        fun toUrlType(): Int = when (this) {
            PRIVACY -> 21
            TERMS -> 22
            ABOUT -> 23
        }
    }

    /**
     * Get policy URL with modern error handling and state management
     */
    fun getUrl(type: Int) {
        val policyType = PolicyType.values().find { it.value == type } ?: PolicyType.PRIVACY
        getPolicy(policyType)
    }

    /**
     * Get policy with type-safe enum
     */
    fun getPolicy(type: PolicyType) {
        launchWithLoading {
            try {
                _policyState.value = PolicyState.Loading

                val result = policyRepository.getPolicyContent(type)

                when (result) {
                    is BaseRepository.Result.Success -> {
                        _policyState.value = PolicyState.Success(result.data)
                    }

                    is BaseRepository.Result.Error -> {
                        val errorMessage = result.exception.message ?: "Failed to load policy"
                        _policyState.value = PolicyState.Error(errorMessage)
                        _events.emit(PolicyEvent.ShowError(errorMessage))
                    }

                    is BaseRepository.Result.Loading -> {
                        // Already handled above
                    }
                }
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Unknown error occurred"
                _policyState.value = PolicyState.Error(errorMessage)
                _events.emit(PolicyEvent.ShowError(errorMessage))
            }
        }
    }

    /**
     * Retry loading policy
     */
    fun retry() {
        val currentState = _policyState.value
        if (currentState is PolicyState.Error) {
            getPolicy(PolicyType.PRIVACY) // Default retry with privacy policy
        }
    }

    /**
     * Clear error state - renamed to avoid conflict with BaseViewModel
     */
    fun clearPolicyError() {
        clearError()
        if (_policyState.value is PolicyState.Error) {
            _policyState.value = PolicyState.Idle
        }
    }

    /**
     * Repository for policy data operations
     */
    private inner class PolicyRepository : BaseRepository() {

        private val cacheKey = "policy_content"

        suspend fun getPolicyContent(type: PolicyType): BaseRepository.Result<HtmlBean> = safeCall {
            val key = "${cacheKey}_${type.value}"

            // Cache for 1 hour
            getCachedOrExecute(key, 60 * 60 * 1000L) {
                fetchPolicyFromNetwork(type)
            }
        }

        private suspend fun fetchPolicyFromNetwork(type: PolicyType): HtmlBean {
            val urlType = type.toUrlType()
            val result = LmsRepository.getStatementUrl(urlType.toString())

            return if (result != null && !result.htmlContent.isNullOrBlank()) {
                HtmlBean(
                    body = result.htmlContent,
                    action = 1,
                    title = getTitleForType(type),
                    type = type
                )
            } else {
                throw Exception("No content available for ${type.name.lowercase()} policy")
            }
        }

        private fun getTitleForType(type: PolicyType): String {
            return when (type) {
                PolicyType.PRIVACY -> "Privacy Policy"
                PolicyType.TERMS -> "Terms of Service"
                PolicyType.ABOUT -> "About"
            }
        }
    }

    companion object {
        private const val TAG = "PolicyViewModel"
    }
}
