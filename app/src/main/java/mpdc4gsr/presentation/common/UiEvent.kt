package mpdc4gsr.presentation.common.domain.model

sealed class UiEvent {
    object ShowExitDialog : UiEvent()
    data class ShowToast(val message: String, val isLong: Boolean = false) : UiEvent()
}
