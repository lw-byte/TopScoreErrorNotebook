package com.topscore.errornotebook.ui.home

import com.topscore.errornotebook.domain.model.Question

/**
 * 首页 UI 状态
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val errorCount: Int = 0,
    val inProgressCount: Int = 0,
    val masteredCount: Int = 0,
    val todayCount: Int = 0,
    val recentQuestions: List<Question> = emptyList(),
    val errorMessage: String? = null
)

/**
 * 首页事件
 */
sealed class HomeEvent {
    data object LoadHomeData : HomeEvent()
    data class NavigateToQuestionDetail(val questionId: Long) : HomeEvent()
}