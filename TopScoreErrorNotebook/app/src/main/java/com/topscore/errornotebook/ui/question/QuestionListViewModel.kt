package com.topscore.errornotebook.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topscore.errornotebook.core.database.dao.QuestionDao
import com.topscore.errornotebook.core.database.toDomain
import com.topscore.errornotebook.domain.model.Question
import com.topscore.errornotebook.domain.model.QuestionFilter
import com.topscore.errornotebook.domain.model.QuestionStatus
import com.topscore.errornotebook.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Question List ViewModel
 * Handles LoadQuestions, ApplyFilter, Search, ToggleSelection, DeleteSelected events
 */
@HiltViewModel
class QuestionListViewModel @Inject constructor(
    private val questionDao: QuestionDao
) : ViewModel() {

    // Current user ID (normally obtained from session management)
    private val userId: Long = 1L

    private val _uiState = MutableStateFlow(QuestionListUiState())
    val uiState: StateFlow<QuestionListUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<QuestionListEvent?>(null)
    val events: StateFlow<QuestionListEvent?> = _events.asStateFlow()

    init {
        loadQuestions()
    }

    /**
     * Handle UI events
     */
    fun onEvent(event: QuestionListEvent) {
        Logger.Question.i("onEvent: $event")
        when (event) {
            is QuestionListEvent.LoadQuestions -> loadQuestions()
            is QuestionListEvent.ApplyFilter -> applyFilter(event.filter)
            is QuestionListEvent.Search -> search(event.keyword)
            is QuestionListEvent.ToggleSelection -> toggleSelection(event.questionId)
            is QuestionListEvent.DeleteSelected -> deleteSelected()
            is QuestionListEvent.ToggleSelectionMode -> toggleSelectionMode()
            is QuestionListEvent.ClearSelection -> clearSelection()
            is QuestionListEvent.ConsumeEvent -> _events.value = null
            is QuestionListEvent.ShowMessage -> { /* Handled externally */ }
            is QuestionListEvent.ShowError -> { /* Handled externally */ }
        }
    }

    private fun loadQuestions() {
        Logger.Question.i("loadQuestions: starting")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                questionDao.observeQuestions(userId, QuestionStatus.ACTIVE.name)
                    .collect { entities ->
                        val questions = entities.map { entity -> entity.toDomain() }
                        Logger.Question.i("loadQuestions: loaded ${questions.size} questions")
                        _uiState.update { state ->
                            state.copy(
                                isLoading = false,
                                questions = questions,
                                filteredQuestions = filterQuestions(questions, state.filter, state.searchKeyword)
                            )
                        }
                    }
            } catch (e: Exception) {
                Logger.Question.e("loadQuestions failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun applyFilter(filter: QuestionFilter) {
        Logger.Question.i("applyFilter: stage=${filter.stage}, subject=${filter.subject}, errorReasons=${filter.errorReasons}")
        _uiState.update { state ->
            val filtered = filterQuestions(state.questions, filter, state.searchKeyword)
            Logger.Question.i("applyFilter: filtered ${filtered.size} questions")
            state.copy(
                filter = filter,
                filteredQuestions = filtered
            )
        }
    }

    private fun search(keyword: String) {
        Logger.Question.i("search: keyword=$keyword")
        _uiState.update { state ->
            val filtered = filterQuestions(state.questions, state.filter, keyword)
            Logger.Question.i("search: found ${filtered.size} results")
            state.copy(
                searchKeyword = keyword,
                filteredQuestions = filtered
            )
        }
    }

    private fun filterQuestions(
        questions: List<Question>,
        filter: QuestionFilter,
        keyword: String
    ): List<Question> {
        return questions.filter { question ->
            val matchesStage = filter.stage == null || question.stage == filter.stage
            val matchesSubject = filter.subject.isNullOrBlank() || question.subject.contains(filter.subject, ignoreCase = true)
            val matchesErrorReason = filter.errorReasons.isNullOrEmpty() || question.errorReason in filter.errorReasons
            val matchesKeyword = keyword.isBlank() ||
                    (question.notes?.contains(keyword, ignoreCase = true) == true) ||
                    question.subject.contains(keyword, ignoreCase = true) ||
                    question.errorReason.name.contains(keyword, ignoreCase = true)
            matchesStage && matchesSubject && matchesErrorReason && matchesKeyword
        }
    }

    private fun toggleSelection(questionId: Long) {
        Logger.Question.i("toggleSelection: questionId=$questionId")
        _uiState.update { state ->
            val newSelectedIds = if (questionId in state.selectedIds) {
                state.selectedIds - questionId
            } else {
                state.selectedIds + questionId
            }
            state.copy(
                selectedIds = newSelectedIds,
                isSelectionMode = newSelectedIds.isNotEmpty()
            )
        }
    }

    private fun deleteSelected() {
        viewModelScope.launch {
            val selectedIds = _uiState.value.selectedIds.toList()
            Logger.Question.i("deleteSelected: ${selectedIds.size} questions")
            try {
                questionDao.deleteQuestions(selectedIds)
                _uiState.update { state ->
                    state.copy(
                        selectedIds = emptySet(),
                        isSelectionMode = false
                    )
                }
                _events.value = QuestionListEvent.ShowMessage("已删除 ${selectedIds.size} 道错题")
            } catch (e: Exception) {
                Logger.Question.e("deleteSelected failed: ${e.message}", e)
                _events.value = QuestionListEvent.ShowError(e.message ?: "删除失败")
            }
        }
    }

    private fun toggleSelectionMode() {
        Logger.Question.i("toggleSelectionMode")
        _uiState.update { state ->
            state.copy(
                isSelectionMode = !state.isSelectionMode,
                selectedIds = if (state.isSelectionMode) emptySet() else state.selectedIds
            )
        }
    }

    private fun clearSelection() {
        Logger.Question.i("clearSelection")
        _uiState.update { state ->
            state.copy(
                selectedIds = emptySet(),
                isSelectionMode = false
            )
        }
    }
}

/**
 * UI State for Question List
 */
data class QuestionListUiState(
    val isLoading: Boolean = false,
    val questions: List<Question> = emptyList(),
    val filteredQuestions: List<Question> = emptyList(),
    val filter: QuestionFilter = QuestionFilter(),
    val searchKeyword: String = "",
    val selectedIds: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val error: String? = null
)

/**
 * Events for Question List
 */
sealed class QuestionListEvent {
    data object LoadQuestions : QuestionListEvent()
    data class ApplyFilter(val filter: QuestionFilter) : QuestionListEvent()
    data class Search(val keyword: String) : QuestionListEvent()
    data class ToggleSelection(val questionId: Long) : QuestionListEvent()
    data object DeleteSelected : QuestionListEvent()
    data object ToggleSelectionMode : QuestionListEvent()
    data object ClearSelection : QuestionListEvent()
    data object ConsumeEvent : QuestionListEvent()

    data class ShowMessage(val message: String) : QuestionListEvent()
    data class ShowError(val error: String) : QuestionListEvent()
}