package com.topscore.errornotebook.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topscore.errornotebook.data.sync.CloudSyncService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

data class User(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val avatarUrl: String? = null
)

enum class ProfileSyncStatus {
    IDLE, SYNCING, SUCCESS, FAILURE
}

data class ProfileState(
    val user: User = User(),
    val syncStatus: ProfileSyncStatus = ProfileSyncStatus.IDLE,
    val lastSyncTime: Instant? = null,
    val isLoading: Boolean = false,
    val syncErrorMessage: String? = null
)

sealed class ProfileEvent {
    data object LoadProfile : ProfileEvent()
    data object SyncNow : ProfileEvent()
    data object Logout : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val cloudSyncService: CloudSyncService
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    // TODO: Get from UserSession
    private val authToken: String = "placeholder_auth_token"

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoadProfile -> loadProfile()
            is ProfileEvent.SyncNow -> syncNow()
            is ProfileEvent.Logout -> logout()
        }
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // TODO: load from repository
            _state.update {
                it.copy(
                    user = User(
                        id = "1",
                        name = "User",
                        phone = "138****8888"
                    ),
                    isLoading = false
                )
            }
        }
    }

    private fun syncNow() {
        viewModelScope.launch {
            _state.update { it.copy(syncStatus = ProfileSyncStatus.SYNCING, syncErrorMessage = null) }
            try {
                val result = cloudSyncService.sync(
                    authToken = authToken,
                    mode = CloudSyncService.SyncMode.INCREMENTAL
                )

                if (result.success) {
                    _state.update {
                        it.copy(
                            syncStatus = ProfileSyncStatus.SUCCESS,
                            lastSyncTime = Instant.now()
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            syncStatus = ProfileSyncStatus.FAILURE,
                            syncErrorMessage = result.errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        syncStatus = ProfileSyncStatus.FAILURE,
                        syncErrorMessage = e.message
                    )
                }
            }
        }
    }

    private fun logout() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // TODO: perform logout
            _state.update { ProfileState() }
        }
    }
}
