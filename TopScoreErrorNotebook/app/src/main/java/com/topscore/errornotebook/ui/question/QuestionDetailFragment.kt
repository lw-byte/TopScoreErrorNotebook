package com.topscore.errornotebook.ui.question

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topscore.errornotebook.R
import com.topscore.errornotebook.databinding.FragmentQuestionDetailBinding
import com.topscore.errornotebook.domain.model.ErrorReason
import com.topscore.errornotebook.domain.model.Question
import com.topscore.errornotebook.domain.model.SubjectStage
import com.topscore.errornotebook.domain.model.SyncStatus
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Question Detail Fragment
 * Displays full question details including image, answers comparison, notes, tags, and metadata
 */
@AndroidEntryPoint
class QuestionDetailFragment : Fragment() {

    private var _binding: FragmentQuestionDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuestionDetailViewModel by viewModels()
    private val args: QuestionDetailFragmentArgs by navArgs()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        observeState()
        // Load the question
        viewModel.onEvent(QuestionDetailEvent.LoadQuestion(args.questionId))
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEdit.setOnClickListener {
            // Navigate to edit screen (to be implemented)
            Toast.makeText(requireContext(), "编辑功能待实现", Toast.LENGTH_SHORT).show()
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        binding.cardImage.setOnClickListener {
            // Navigate to fullscreen image viewer (to be implemented)
            Toast.makeText(requireContext(), "图片预览待实现", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete)
            .setMessage(R.string.delete_confirm)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.delete) { _, _ ->
                viewModel.onEvent(QuestionDetailEvent.DeleteQuestion)
            }
            .show()
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUi(state)
                    }
                }
                launch {
                    viewModel.events.collect { event ->
                        handleEvent(event)
                    }
                }
            }
        }
    }

    private fun updateUi(state: QuestionDetailUiState) {
        binding.loadingOverlay.isVisible = state.isLoading

        state.question?.let { question ->
            bindQuestionData(question)
        }

        state.questionImage?.let { image ->
            loadImage(image.originalImageLocal)
        }

        state.errorMessage?.let { error ->
            Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        }
    }

    private fun bindQuestionData(question: Question) {
        // Stage badge
        binding.tvStage.text = getStageText(question.stage)
        binding.tvStage.setBackgroundResource(getStageBackground(question.stage))

        // Subject
        binding.tvSubject.text = question.subject

        // Error reason
        binding.tvErrorReason.text = getErrorReasonText(question.errorReason)

        // Source
        if (!question.source.isNullOrBlank()) {
            binding.tvSource.text = getString(R.string.source_info) + "：" + question.source
            binding.tvSource.isVisible = true
        } else {
            binding.tvSource.isVisible = false
        }

        // Question text (recognized from image or entered manually)
        val questionText = question.notes?.takeIf { it.isNotBlank() } ?: "题目图片见上方"
        binding.tvQuestionText.text = questionText

        // My answer
        if (!question.wrongAnswer.isNullOrBlank()) {
            binding.tvMyAnswer.text = question.wrongAnswer
            binding.tvMyAnswer.isVisible = true
        } else {
            binding.tvMyAnswer.text = "未填写"
            binding.tvMyAnswer.isVisible = true
        }

        // Correct answer
        if (!question.correctAnswer.isNullOrBlank()) {
            binding.tvCorrectAnswer.text = question.correctAnswer
            binding.tvCorrectAnswer.isVisible = true
        } else {
            binding.tvCorrectAnswer.text = "未填写"
            binding.tvCorrectAnswer.isVisible = true
        }

        // Notes
        if (!question.notes.isNullOrBlank()) {
            binding.tvNotesLabel.isVisible = true
            binding.tvNotes.isVisible = true
            binding.tvNotes.text = question.notes
        } else {
            binding.tvNotesLabel.isVisible = false
            binding.tvNotes.isVisible = false
        }

        // Tags
        if (question.tags.isNotEmpty()) {
            binding.tvTagsLabel.isVisible = true
            binding.chipGroupTags.isVisible = true
            binding.chipGroupTags.removeAllViews()
            question.tags.forEach { tag ->
                val chip = Chip(requireContext()).apply {
                    text = tag
                    isClickable = false
                    setChipBackgroundColorResource(R.color.primary_light)
                    setTextColor(resources.getColor(android.R.color.white, null))
                }
                binding.chipGroupTags.addView(chip)
            }
        } else {
            binding.tvTagsLabel.isVisible = false
            binding.chipGroupTags.isVisible = false
        }

        // Error date
        binding.tvErrorDate.text = dateFormat.format(Date(question.errorDate))

        // Sync status
        binding.tvSyncStatus.text = "状态：" + getSyncStatusText(question.syncStatus)
    }

    private fun loadImage(imagePath: String) {
        if (imagePath.isBlank()) {
            binding.cardImage.isVisible = false
            return
        }

        val file = File(imagePath)
        if (file.exists()) {
            binding.cardImage.isVisible = true
            val bitmap = BitmapFactory.decodeFile(imagePath)
            binding.ivQuestionImage.setImageBitmap(bitmap)
        } else {
            binding.cardImage.isVisible = false
        }
    }

    private fun handleEvent(event: QuestionDetailEvent?) {
        when (event) {
            is QuestionDetailEvent.NavigateBack -> {
                findNavController().navigateUp()
                viewModel.onEvent(QuestionDetailEvent.ConsumeEvent)
            }
            is QuestionDetailEvent.ShowError -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                viewModel.onEvent(QuestionDetailEvent.ConsumeEvent)
            }
            else -> {}
        }
    }

    private fun getStageText(stage: SubjectStage): String {
        return when (stage) {
            SubjectStage.PRIMARY -> getString(R.string.stage_primary)
            SubjectStage.MIDDLE -> getString(R.string.stage_middle)
            SubjectStage.HIGH -> getString(R.string.stage_high)
        }
    }

    private fun getStageBackground(stage: SubjectStage): Int {
        return when (stage) {
            SubjectStage.PRIMARY -> R.drawable.bg_stage_primary
            SubjectStage.MIDDLE -> R.drawable.bg_stage_middle
            SubjectStage.HIGH -> R.drawable.bg_stage_high
        }
    }

    private fun getErrorReasonText(reason: ErrorReason): String {
        return when (reason) {
            ErrorReason.MISREAD -> getString(R.string.error_misread)
            ErrorReason.CALC_ERROR -> getString(R.string.error_calc)
            ErrorReason.CONCEPT_UNCLEAR -> getString(R.string.error_concept)
            ErrorReason.KNOWLEDGE_GAP -> getString(R.string.error_knowledge)
            ErrorReason.CARELESS -> getString(R.string.error_careless)
            ErrorReason.OTHER -> getString(R.string.error_other)
        }
    }

    private fun getSyncStatusText(status: SyncStatus): String {
        return when (status) {
            SyncStatus.PENDING -> "待同步"
            SyncStatus.SYNCED -> "已同步"
            SyncStatus.FAILED -> "同步失败"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}