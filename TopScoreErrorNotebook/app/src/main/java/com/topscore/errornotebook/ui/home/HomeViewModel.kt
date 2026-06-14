package com.topscore.errornotebook.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topscore.errornotebook.core.database.dao.QuestionDao
import com.topscore.errornotebook.core.database.toDomain
import com.topscore.errornotebook.domain.model.Question
import com.topscore.errornotebook.domain.model.QuestionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val questionDao: QuestionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<HomeEvent.NavigateToQuestionDetail>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // TODO: 从 UserSession 或 Repository 获取当前用户ID
    private val currentUserId: Long = 1L

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.LoadHomeData -> loadHomeData()
            is HomeEvent.NavigateToQuestionDetail -> navigateToQuestionDetail(event.questionId)
        }
    }

    private fun loadHomeData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 获取总错题数 (ACTIVE 状态)
                val errorCount = questionDao.getActiveQuestionCount(currentUserId)

                // 获取进行中的数量 (ACTIVE 状态中的一部分，这里简化处理)
                // 实际应该根据业务逻辑区分：ACTIVE = 进行中，MASTERED = 已掌握
                val inProgressCount = errorCount

                // 获取已掌握的数量 (MASTERED 状态)
                // 需要通过单独查询或字段区分，这里先设为 0
                val masteredCount = 0

                // 获取今日添加的数量
                val todayStart = getTodayStartMillis()
                val todayQuestions = questionDao.getQuestions(
                    userId = currentUserId,
                    status = QuestionStatus.ACTIVE.name,
                    limit = 100,
                    offset = 0
                ).filter { it.createdAt >= todayStart }
                val todayCount = todayQuestions.size

                // 获取最近添加的错题 (前5条)
                val recentQuestions = questionDao.getQuestions(
                    userId = currentUserId,
                    status = QuestionStatus.ACTIVE.name,
                    limit = 5,
                    offset = 0
                ).map { entity -> entity.toDomain() }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorCount = errorCount,
                        inProgressCount = inProgressCount,
                        masteredCount = masteredCount,
                        todayCount = todayCount,
                        recentQuestions = recentQuestions
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "加载数据失败"
                    )
                }
            }
        }
    }

    private fun navigateToQuestionDetail(questionId: Long) {
        viewModelScope.launch {
            _navigationEvent.emit(HomeEvent.NavigateToQuestionDetail(questionId))
        }
    }

    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}