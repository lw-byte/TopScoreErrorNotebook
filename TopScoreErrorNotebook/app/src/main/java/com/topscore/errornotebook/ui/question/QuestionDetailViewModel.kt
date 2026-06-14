package com.topscore.errornotebook.ui.question

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topscore.errornotebook.core.database.dao.ImageDao
import com.topscore.errornotebook.core.database.dao.QuestionDao
import com.topscore.errornotebook.core.database.entity.QuestionImageEntity
import com.topscore.errornotebook.core.database.toDomain
import com.topscore.errornotebook.domain.model.Question
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Question Detail ViewModel
 * Handles LoadQuestion, DeleteQuestion events
 */
@HiltViewModel
class QuestionDetailViewModel @Inject constructor(
    private val questionDao: QuestionDao,
    private val imageDao: ImageDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionDetailUiState())
    val uiState: StateFlow<QuestionDetailUiState> = _uiState.asStateFlow()

    private val _events = MutableStateFlow<QuestionDetailEvent?>(null)
    val events: StateFlow<QuestionDetailEvent?> = _events.asStateFlow()

    /**
     * Handle UI events
     */
    fun onEvent(event: QuestionDetailEvent) {
        when (event) {
            is QuestionDetailEvent.LoadQuestion -> loadQuestion(event.questionId)
            is QuestionDetailEvent.DeleteQuestion -> deleteQuestion()
            is QuestionDetailEvent.ConsumeEvent -> _events.value = null
            is QuestionDetailEvent.NavigateBack -> { /* Handled externally */ }
            is QuestionDetailEvent.ShowError -> { /* Handled externally */ }
        }
    }

    private fun loadQuestion(questionId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val entity = questionDao.getQuestionById(questionId)
                if (entity != null) {
                    val question = entity.toDomain()

                    // Load associated image if exists
                    val image = if (entity.imageId > 0) {
                        imageDao.getImageById(entity.imageId)
                    } else null

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            question = question,
                            questionImage = image
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "题目不存在") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "加载失败") }
            }
        }
    }

    private fun deleteQuestion() {
        val question = _uiState.value.question ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                questionDao.deleteQuestion(question.id)
                _events.value = QuestionDetailEvent.NavigateBack
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "删除失败") }
                _events.value = QuestionDetailEvent.ShowError(e.message ?: "删除失败")
            }
        }
    }
}

/**
 * UI State for Question Detail
 */
data class QuestionDetailUiState(
    val isLoading: Boolean = false,
    val question: Question? = null,
    val questionImage: QuestionImageEntity? = null,
    val errorMessage: String? = null
)

/**
 * Events for Question Detail
 */
sealed class QuestionDetailEvent {
    data class LoadQuestion(val questionId: Long) : QuestionDetailEvent()
    data object DeleteQuestion : QuestionDetailEvent()
    data object ConsumeEvent : QuestionDetailEvent()

    data object NavigateBack : QuestionDetailEvent()
    data class ShowError(val message: String) : QuestionDetailEvent()
}