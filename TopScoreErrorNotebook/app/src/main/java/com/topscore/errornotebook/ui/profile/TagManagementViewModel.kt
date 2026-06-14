package com.topscore.errornotebook.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Tag(
    val id: String = "",
    val name: String = "",
    val color: String = "#2D5A4A"
)

data class TagManagementState(
    val tags: List<Tag> = emptyList(),
    val isLoading: Boolean = false
)

sealed class TagManagementEvent {
    data object LoadTags : TagManagementEvent()
    data class CreateTag(val name: String) : TagManagementEvent()
    data class DeleteTag(val id: String) : TagManagementEvent()
}

@HiltViewModel
class TagManagementViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(TagManagementState())
    val state: StateFlow<TagManagementState> = _state.asStateFlow()

    fun onEvent(event: TagManagementEvent) {
        when (event) {
            is TagManagementEvent.LoadTags -> loadTags()
            is TagManagementEvent.CreateTag -> createTag(event.name)
            is TagManagementEvent.DeleteTag -> deleteTag(event.id)
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // TODO: load from repository
            _state.update {
                it.copy(
                    tags = listOf(
                        Tag(id = "1", name = "Math", color = "#2D5A4A"),
                        Tag(id = "2", name = "Physics", color = "#C75450")
                    ),
                    isLoading = false
                )
            }
        }
    }

    private fun createTag(name: String) {
        viewModelScope.launch {
            val newTag = Tag(
                id = System.currentTimeMillis().toString(),
                name = name
            )
            _state.update { it.copy(tags = it.tags + newTag) }
        }
    }

    private fun deleteTag(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(tags = it.tags.filter { tag -> tag.id != id }) }
        }
    }
}
