package com.topscore.errornotebook.ui.export

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
import javax.inject.Inject

/**
 * 导出配置数据类
 */
data class ExportConfig(
    val includeQuestion: Boolean = true,
    val includeAnswer: Boolean = true,
    val includeNotes: Boolean = true,
    val includeSource: Boolean = false,
    val includeTags: Boolean = true,
    val answerPlacement: AnswerPlacement = AnswerPlacement.SEPARATE_PAGE,
    val pageOrientation: PageOrientation = PageOrientation.PORTRAIT,
    val spacing: Int = 16, // 题目间留白，单位 dp
    val fontSize: FontSize = FontSize.MEDIUM
)

enum class AnswerPlacement {
    SEPARATE_PAGE,  // 另起一页（背面）
    INTERLEAVED     // 穿插在题后
}

enum class PageOrientation {
    PORTRAIT,   // 纵向
    LANDSCAPE   // 横向
}

enum class FontSize {
    SMALL,
    MEDIUM,
    LARGE
}

/**
 * 导出 UI 状态
 */
data class ExportUiState(
    val isLoading: Boolean = false,
    val selectedQuestions: List<Question> = emptyList(),
    val config: ExportConfig = ExportConfig(),
    val errorMessage: String? = null,
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val previewHtml: String? = null
)

/**
 * 导出事件
 */
sealed class ExportEvent {
    data object LoadQuestions : ExportEvent()
    data class UpdateConfig(val config: ExportConfig) : ExportEvent()
    data object ExportToWord : ExportEvent()
    data object ExportToPdf : ExportEvent()
    data object Print : ExportEvent()
    data object Preview : ExportEvent()
    data class SelectQuestions(val questionIds: List<Long>) : ExportEvent()

    // 导航事件
    data class NavigateToPreview(val html: String) : ExportEvent()
    data object ExportComplete : ExportEvent()
    data class ShowError(val message: String) : ExportEvent()
}

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val questionDao: QuestionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<ExportEvent>()
    val navigationEvent = _navigationEvent.asSharedFlow()

    // TODO: 从 UserSession 或 Repository 获取当前用户ID
    private val currentUserId: Long = 1L

    fun onEvent(event: ExportEvent) {
        when (event) {
            is ExportEvent.LoadQuestions -> loadSelectedQuestions()
            is ExportEvent.UpdateConfig -> updateConfig(event.config)
            is ExportEvent.ExportToWord -> exportToWord()
            is ExportEvent.ExportToPdf -> exportToPdf()
            is ExportEvent.Print -> print()
            is ExportEvent.Preview -> generatePreview()
            is ExportEvent.SelectQuestions -> selectQuestions(event.questionIds)
            is ExportEvent.NavigateToPreview -> { /* Navigation handled externally */ }
            is ExportEvent.ExportComplete -> { /* Handled externally */ }
            is ExportEvent.ShowError -> { /* Handled externally */ }
        }
    }

    private fun loadSelectedQuestions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 获取所有 ACTIVE 状态的错题
                val questions = questionDao.getQuestions(
                    userId = currentUserId,
                    status = QuestionStatus.ACTIVE.name,
                    limit = 1000,
                    offset = 0
                ).map { entity -> entity.toDomain() }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedQuestions = questions
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "加载错题失败"
                    )
                }
            }
        }
    }

    private fun updateConfig(config: ExportConfig) {
        _uiState.update { it.copy(config = config) }
    }

    private fun selectQuestions(questionIds: List<Long>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val questions = questionDao.getQuestions(
                    userId = currentUserId,
                    status = QuestionStatus.ACTIVE.name,
                    limit = 1000,
                    offset = 0
                ).filter { it.id in questionIds }
                    .map { entity -> entity.toDomain() }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        selectedQuestions = questions
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "选择错题失败"
                    )
                }
            }
        }
    }

    private fun generatePreview() {
        viewModelScope.launch {
            val config = _uiState.value.config
            val questions = _uiState.value.selectedQuestions

            if (questions.isEmpty()) {
                _navigationEvent.emit(ExportEvent.ShowError("请先选择要导出的错题"))
                return@launch
            }

            val html = buildPreviewHtml(questions, config)
            _uiState.update { it.copy(previewHtml = html) }
            _navigationEvent.emit(ExportEvent.NavigateToPreview(html))
        }
    }

    private fun buildPreviewHtml(questions: List<Question>, config: ExportConfig): String {
        return buildString {
            append("""
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <style>
                        * { margin: 0; padding: 0; box-sizing: border-box; }
                        body {
                            font-family: "Songti SC", "SimSun", serif;
                            background: #FAF7F2;
                            color: #2C2C2C;
                            padding: 20px;
                            font-size: ${getFontSize(config.fontSize)}px;
                        }
                        .page {
                            background: white;
                            padding: 30px;
                            margin-bottom: 20px;
                            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
                        }
                        .question-item {
                            margin-bottom: ${config.spacing}px;
                            padding-bottom: ${config.spacing}px;
                            border-bottom: 1px solid #E0E0E0;
                        }
                        .question-item:last-child { border-bottom: none; }
                        .question-header {
                            color: #2D5A4A;
                            font-weight: bold;
                            margin-bottom: 8px;
                        }
                        .question-content { margin-bottom: 12px; }
                        .answer {
                            color: #C75450;
                            margin-top: 8px;
                        }
                        .notes {
                            color: #6B6B6B;
                            font-style: italic;
                            margin-top: 8px;
                        }
                        .source {
                            color: #A8A8A8;
                            font-size: 0.9em;
                            margin-top: 8px;
                        }
                        .tags {
                            margin-top: 8px;
                        }
                        .tag {
                            display: inline-block;
                            background: #E8F5E9;
                            color: #2D5A4A;
                            padding: 2px 8px;
                            border-radius: 4px;
                            font-size: 0.85em;
                            margin-right: 4px;
                        }
                        .answer-page {
                            page-break-before: always;
                            padding-top: 50px;
                        }
                        @media print {
                            .page { box-shadow: none; margin: 0; }
                            .answer-page { page-break-before: always; }
                        }
                    </style>
                </head>
                <body>
            """.trimIndent())
        }
    }

    private fun getFontSize(fontSize: FontSize): Int {
        return when (fontSize) {
            FontSize.SMALL -> 12
            FontSize.MEDIUM -> 14
            FontSize.LARGE -> 16
        }
    }

    private fun exportToWord() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportSuccess = false) }

            try {
                // TODO: 实现 Word 导出逻辑
                // 可以使用 Apache POI 或其他库生成 .docx 文件

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportSuccess = true
                    )
                }
                _navigationEvent.emit(ExportEvent.ExportComplete)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = e.message ?: "导出 Word 失败"
                    )
                }
                _navigationEvent.emit(ExportEvent.ShowError(e.message ?: "导出 Word 失败"))
            }
        }
    }

    private fun exportToPdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, exportSuccess = false) }

            try {
                // TODO: 实现 PDF 导出逻辑
                // 可以使用 iText 或 Android Print Framework

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportSuccess = true
                    )
                }
                _navigationEvent.emit(ExportEvent.ExportComplete)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = e.message ?: "导出 PDF 失败"
                    )
                }
                _navigationEvent.emit(ExportEvent.ShowError(e.message ?: "导出 PDF 失败"))
            }
        }
    }

    private fun print() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true) }

            try {
                // TODO: 实现打印逻辑
                // 使用 Android Print Framework

                _uiState.update { it.copy(isExporting = false) }
                _navigationEvent.emit(ExportEvent.ExportComplete)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = e.message ?: "打印失败"
                    )
                }
                _navigationEvent.emit(ExportEvent.ShowError(e.message ?: "打印失败"))
            }
        }
    }
}