package com.topscore.errornotebook.ui.question

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topscore.errornotebook.core.database.dao.ImageDao
import com.topscore.errornotebook.core.database.dao.QuestionDao
import com.topscore.errornotebook.core.database.toEntity
import com.topscore.errornotebook.core.ocr.AlibabaOcrService
import com.topscore.errornotebook.domain.model.ErrorReason
import com.topscore.errornotebook.domain.model.OcrResult
import com.topscore.errornotebook.domain.model.Question
import com.topscore.errornotebook.domain.model.QuestionStatus
import com.topscore.errornotebook.domain.model.QuestionType
import com.topscore.errornotebook.domain.model.SubjectStage
import com.topscore.errornotebook.domain.model.SyncStatus
import com.topscore.errornotebook.util.Logger
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
 * Add Question Flow Steps
 */
enum class AddQuestionStep {
    SELECT_SOURCE,     // Step 1: Select image source (camera/album)
    CAMERA_CAPTURE,   // Step 2: Camera capture
    OCR_RECOGNIZING,   // Step 3: OCR recognizing
    CONFIRM_RESULT,    // Step 4: Confirm OCR result
    FILL_INFO          // Step 5: Fill in question info form
}

/**
 * Add Question UI State
 */
data class AddQuestionUiState(
    val currentStep: AddQuestionStep = AddQuestionStep.SELECT_SOURCE,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,

    // Image capture
    val imagePath: String? = null,
    val imageUri: Uri? = null,

    // OCR
    val ocrResult: OcrResult? = null,

    // Form fields
    val stage: SubjectStage = SubjectStage.MIDDLE,
    val subject: String = "",
    val errorReason: ErrorReason = ErrorReason.CALC_ERROR,
    val source: String = "",
    val questionType: QuestionType = QuestionType.CHOICE,
    val correctAnswer: String = "",
    val wrongAnswer: String = "",
    val notes: String = "",
    val tags: List<String> = emptyList(),
    val errorDate: Long = System.currentTimeMillis()
)

/**
 * Add Question Events
 */
sealed class AddQuestionEvent {
    data object SaveSuccess : AddQuestionEvent()
    data class ShowError(val message: String) : AddQuestionEvent()
    data object NavigateBack : AddQuestionEvent()
}

/**
 * Add Question ViewModel
 */
@HiltViewModel
class AddQuestionViewModel @Inject constructor(
    private val questionDao: QuestionDao,
    private val imageDao: ImageDao,
    private val ocrService: AlibabaOcrService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddQuestionUiState())
    val uiState: StateFlow<AddQuestionUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AddQuestionEvent>()
    val events = _events.asSharedFlow()

    // TODO: Get from UserSession
    private val currentUserId: Long = 1L

    /**
     * Handle Add Question Events
     */
    fun onEvent(event: AddQuestionEvent) {
        when (event) {
            is AddQuestionEvent.SaveSuccess -> {
                Logger.Question.i("SaveSuccess event received")
                viewModelScope.launch {
                    _events.emit(AddQuestionEvent.SaveSuccess)
                }
            }
            is AddQuestionEvent.ShowError -> {
                Logger.Question.e("ShowError: ${event.message}")
                viewModelScope.launch {
                    _events.emit(AddQuestionEvent.ShowError(event.message))
                }
            }
            is AddQuestionEvent.NavigateBack -> {
                Logger.Question.i("NavigateBack event received")
                viewModelScope.launch {
                    _events.emit(AddQuestionEvent.NavigateBack)
                }
            }
        }
    }

    /**
     * Select image source
     */
    fun selectSource(isCamera: Boolean) {
        Logger.Question.i("selectSource: isCamera=$isCamera")
        if (isCamera) {
            _uiState.update { it.copy(currentStep = AddQuestionStep.CAMERA_CAPTURE) }
        } else {
            // Album selection handled in Fragment, will call setImageUri
            _uiState.update { it.copy(currentStep = AddQuestionStep.SELECT_SOURCE) }
        }
    }

    /**
     * Set image from camera capture
     */
    fun setImageFromCamera(imagePath: String) {
        Logger.Question.i("setImageFromCamera: imagePath=$imagePath")
        _uiState.update {
            it.copy(
                imagePath = imagePath,
                currentStep = AddQuestionStep.OCR_RECOGNIZING
            )
        }
        performOcr(imagePath)
    }

    /**
     * Set image from album
     */
    fun setImageFromAlbum(uri: Uri) {
        Logger.Question.i("setImageFromAlbum: uri=$uri")
        _uiState.update {
            it.copy(
                imageUri = uri,
                currentStep = AddQuestionStep.OCR_RECOGNIZING
            )
        }
        // OCR will be performed with URI
    }

    /**
     * Perform OCR on the captured image using Alibaba OCR API
     */
    private fun performOcr(imagePath: String) {
        Logger.OCR.i("Starting OCR for: $imagePath")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // Call Alibaba OCR service
                val result = ocrService.recognizeText(imagePath)

                result.fold(
                    onSuccess = { ocrResult ->
                        Logger.OCR.i("OCR success: confidence=${ocrResult.confidence}, textLength=${ocrResult.text.length}, errorCode=${ocrResult.errorCode}")
                        _uiState.update {
                            it.copy(
                                ocrResult = ocrResult,
                                isLoading = false,
                                currentStep = AddQuestionStep.CONFIRM_RESULT,
                                errorMessage = if (ocrResult.errorCode != null) {
                                    "识别失败:\n<Code>${ocrResult.errorCode}</Code>\n<Message>${ocrResult.errorMessage}</Message>"
                                } else null
                            )
                        }
                    },
                    onFailure = { e ->
                        Logger.OCR.w("OCR failed: ${e.message}, using mock data")
                        // Fallback to mock result if Alibaba OCR fails
                        // In production, you might want to handle this differently
                        val mockOcrResult = OcrResult(
                            text = "这是一道数学选择题。\n题目：下列哪个是正确的是？\nA. 1+1=2\nB. 1+1=3\nC. 1+1=4\nD. 1+1=5",
                            isComplete = true,
                            hasHandwriting = false,
                            handwritingRegions = null,
                            confidence = 0.95f,
                            errorCode = "ServiceUnavailable",
                            errorMessage = e.message
                        )
                        _uiState.update {
                            it.copy(
                                ocrResult = mockOcrResult,
                                isLoading = false,
                                currentStep = AddQuestionStep.CONFIRM_RESULT,
                                errorMessage = "OCR识别服务暂不可用，已使用模拟数据：${e.message}"
                            )
                        }
                    }
                )
            } catch (e: Exception) {
                Logger.OCR.e("OCR exception: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "OCR识别失败",
                        currentStep = AddQuestionStep.CONFIRM_RESULT
                    )
                }
            }
        }
    }

    /**
     * Retake photo - go back to camera capture
     */
    fun retake() {
        Logger.Question.i("retake")
        _uiState.update {
            it.copy(
                imagePath = null,
                imageUri = null,
                ocrResult = null,
                currentStep = AddQuestionStep.CAMERA_CAPTURE
            )
        }
    }

    /**
     * Use OCR result and proceed to fill info
     */
    fun useOcrResult() {
        Logger.Question.i("useOcrResult")
        _uiState.update { it.copy(currentStep = AddQuestionStep.FILL_INFO) }
    }

    /**
     * Edit OCR result - go back to confirm
     */
    fun editOcrResult() {
        Logger.Question.i("editOcrResult")
        _uiState.update { it.copy(currentStep = AddQuestionStep.CONFIRM_RESULT) }
    }

    /**
     * Update form field - stage
     */
    fun updateStage(stage: SubjectStage) {
        _uiState.update { it.copy(stage = stage) }
    }

    /**
     * Update form field - subject
     */
    fun updateSubject(subject: String) {
        _uiState.update { it.copy(subject = subject) }
    }

    /**
     * Update form field - error reason
     */
    fun updateErrorReason(reason: ErrorReason) {
        _uiState.update { it.copy(errorReason = reason) }
    }

    /**
     * Update form field - source
     */
    fun updateSource(source: String) {
        _uiState.update { it.copy(source = source) }
    }

    /**
     * Update form field - question type
     */
    fun updateQuestionType(type: QuestionType) {
        _uiState.update { it.copy(questionType = type) }
    }

    /**
     * Update form field - correct answer
     */
    fun updateCorrectAnswer(answer: String) {
        _uiState.update { it.copy(correctAnswer = answer) }
    }

    /**
     * Update form field - wrong answer
     */
    fun updateWrongAnswer(answer: String) {
        _uiState.update { it.copy(wrongAnswer = answer) }
    }

    /**
     * Update form field - notes
     */
    fun updateNotes(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    /**
     * Update form field - tags
     */
    fun updateTags(tags: List<String>) {
        _uiState.update { it.copy(tags = tags) }
    }

    /**
     * Update form field - error date
     */
    fun updateErrorDate(date: Long) {
        _uiState.update { it.copy(errorDate = date) }
    }

    /**
     * Save the question
     */
    fun saveQuestion() {
        viewModelScope.launch {
            val state = _uiState.value

            // Validation - trim whitespace
            val subject = state.subject.trim()
            Logger.Question.i("saveQuestion: subject=$subject, stage=${state.stage}")
            if (subject.isBlank()) {
                Logger.Question.w("saveQuestion failed: subject is blank")
                _events.emit(AddQuestionEvent.ShowError("请输入科目"))
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }

            try {
                // Save image first
                val imageId = saveImage(state)
                Logger.Question.i("saveQuestion: imageId=$imageId")

                // Create question
                val question = Question(
                    userId = currentUserId,
                    imageId = imageId,
                    stage = state.stage,
                    subject = subject,
                    errorReason = state.errorReason,
                    source = state.source.ifBlank { null },
                    questionType = state.questionType,
                    errorDate = state.errorDate,
                    correctAnswer = state.correctAnswer.ifBlank { null },
                    wrongAnswer = state.wrongAnswer.ifBlank { null },
                    notes = state.notes.ifBlank { null },
                    tags = state.tags,
                    status = QuestionStatus.ACTIVE,
                    syncStatus = SyncStatus.PENDING
                )

                questionDao.insertQuestion(question.toEntity())
                Logger.Question.i("saveQuestion: question saved successfully")

                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AddQuestionEvent.SaveSuccess)

            } catch (e: Exception) {
                Logger.Question.e("saveQuestion failed: ${e.message}", e)
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(AddQuestionEvent.ShowError(e.message ?: "保存失败"))
            }
        }
    }

    /**
     * Save image to database
     */
    private suspend fun saveImage(state: AddQuestionUiState): Long {
        // In production, would upload image and get URL
        // For now, save local path reference
        return 0L
    }

    /**
     * Go back to previous step
     */
    fun goBack() {
        val currentStep = _uiState.value.currentStep
        Logger.Question.i("goBack: currentStep=$currentStep")
        val previousStep = when (currentStep) {
            AddQuestionStep.SELECT_SOURCE -> null
            AddQuestionStep.CAMERA_CAPTURE -> AddQuestionStep.SELECT_SOURCE
            AddQuestionStep.OCR_RECOGNIZING -> AddQuestionStep.CAMERA_CAPTURE
            AddQuestionStep.CONFIRM_RESULT -> AddQuestionStep.OCR_RECOGNIZING
            AddQuestionStep.FILL_INFO -> AddQuestionStep.CONFIRM_RESULT
        }

        if (previousStep != null) {
            _uiState.update { it.copy(currentStep = previousStep) }
        } else {
            viewModelScope.launch {
                _events.emit(AddQuestionEvent.NavigateBack)
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
