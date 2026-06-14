package com.topscore.errornotebook.ui.question

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.topscore.errornotebook.R
import com.topscore.errornotebook.databinding.ItemQuestionBinding
import com.topscore.errornotebook.domain.model.ErrorReason
import com.topscore.errornotebook.domain.model.Question
import com.topscore.errornotebook.domain.model.SubjectStage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Question Adapter with DiffUtil for efficient list updates
 */
class QuestionAdapter(
    private val onItemClick: (Question) -> Unit,
    private val onItemLongClick: (Question) -> Unit
) : ListAdapter<Question, QuestionAdapter.QuestionViewHolder>(QuestionDiffCallback()) {

    private var selectedIds: Set<Long> = emptySet()
    private var isSelectionMode: Boolean = false

    fun submitList(list: List<Question>, selected: Set<Long>, selectionMode: Boolean) {
        selectedIds = selected
        isSelectionMode = selectionMode
        submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuestionViewHolder(
        private val binding: ItemQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(question: Question) {
            binding.apply {
                // Question text/notes preview
                tvQuestionText.text = question.notes?.take(100) ?: "无题目内容"

                // Stage badge
                tvStage.text = getStageText(question.stage)
                tvStage.setBackgroundResource(getStageBackground(question.stage))

                // Subject
                tvSubject.text = question.subject

                // Error reason
                tvErrorReason.text = getErrorReasonText(question.errorReason)

                // Date
                tvDate.text = dateFormat.format(Date(question.errorDate))

                // Selection mode
                checkbox.isVisible = isSelectionMode
                checkbox.isChecked = question.id in selectedIds

                // Click listeners
                root.setOnClickListener { onItemClick(question) }
                root.setOnLongClickListener {
                    onItemLongClick(question)
                    true
                }

                // Visual feedback for selection
                root.isSelected = question.id in selectedIds
            }
        }

        private fun getStageText(stage: SubjectStage): String {
            return when (stage) {
                SubjectStage.PRIMARY -> binding.root.context.getString(R.string.stage_primary)
                SubjectStage.MIDDLE -> binding.root.context.getString(R.string.stage_middle)
                SubjectStage.HIGH -> binding.root.context.getString(R.string.stage_high)
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
                ErrorReason.MISREAD -> binding.root.context.getString(R.string.error_misread)
                ErrorReason.CALC_ERROR -> binding.root.context.getString(R.string.error_calc)
                ErrorReason.CONCEPT_UNCLEAR -> binding.root.context.getString(R.string.error_concept)
                ErrorReason.KNOWLEDGE_GAP -> binding.root.context.getString(R.string.error_knowledge)
                ErrorReason.CARELESS -> binding.root.context.getString(R.string.error_careless)
                ErrorReason.OTHER -> binding.root.context.getString(R.string.error_other)
            }
        }
    }

    /**
     * DiffUtil ItemCallback for efficient list updates
     */
    class QuestionDiffCallback : DiffUtil.ItemCallback<Question>() {
        override fun areItemsTheSame(oldItem: Question, newItem: Question): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Question, newItem: Question): Boolean {
            return oldItem == newItem
        }
    }
}