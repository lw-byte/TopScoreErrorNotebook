package com.topscore.errornotebook.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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

enum class SyncStatus {
    IDLE, SYNCING, SUCCESS, FAILURE
}

data class ProfileState(
    val user: User = User(),
    val syncStatus: SyncStatus = SyncStatus.IDLE,
    val lastSyncTime: Instant? = null,
    val isLoading: Boolean = false
)

sealed class ProfileEvent {
    data object LoadProfile : ProfileEvent()
    data object SyncNow : ProfileEvent()
    data object Logout : ProfileEvent()
}

@HiltViewModel
class ProfileViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

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
            _state.update { it.copy(syncStatus = SyncStatus.SYNCING) }
            try {
                // TODO: actual sync logic
                delay(1500)
                _state.update {
                    it.copy(
                        syncStatus = SyncStatus.SUCCESS,
                        lastSyncTime = Instant.now()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(syncStatus = SyncStatus.FAILURE) }
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
