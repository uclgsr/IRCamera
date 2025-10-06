package mpdc4gsr.feature.settings.presentation

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mpdc4gsr.core.ui.AppBaseViewModel

data class ProfileData(
    val userName: String = "Research Participant",
    val userId: String = "RP-2025-001",
    val email: String = "participant@research.edu",
    val institution: String = "University Research Lab",
    val researchArea: String = "Physiological Computing",
    val bio: String = "Conducting multi-modal sensor research",
    val profilePhotoUrl: String? = null,
    val isProfileVisible: Boolean = true,
    val allowDataSharing: Boolean = false
)

class ProfileEditViewModel : AppBaseViewModel() {
    private val _profileData = MutableStateFlow(ProfileData())
    val profileData: StateFlow<ProfileData> = _profileData.asStateFlow()
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    fun updateUserName(value: String) {
        _profileData.value = _profileData.value.copy(userName = value)
    }

    fun updateUserId(value: String) {
        _profileData.value = _profileData.value.copy(userId = value)
    }

    fun updateEmail(value: String) {
        _profileData.value = _profileData.value.copy(email = value)
    }

    fun updateInstitution(value: String) {
        _profileData.value = _profileData.value.copy(institution = value)
    }

    fun updateResearchArea(value: String) {
        _profileData.value = _profileData.value.copy(researchArea = value)
    }

    fun updateBio(value: String) {
        _profileData.value = _profileData.value.copy(bio = value)
    }

    fun updateProfileVisibility(visible: Boolean) {
        _profileData.value = _profileData.value.copy(isProfileVisible = visible)
    }

    fun updateDataSharing(enabled: Boolean) {
        _profileData.value = _profileData.value.copy(allowDataSharing = enabled)
    }

    fun saveProfile(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // TODO: Save to repository or SharedPreferences
                // For now, just simulate save delay
                kotlinx.coroutines.delay(500)
                onSuccess()
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
