package com.topscore.errornotebook.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.topscore.errornotebook.R
import com.topscore.errornotebook.databinding.ItemRecentQuestionBinding
import com.topscore.errornotebook.domain.model.ErrorReason
import com.topscore.errornotebook.domain.model.Question
import com.topscore.errornotebook.domain.model.QuestionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecentQuestionsAdapter(
    private val onItemClick: (Question) -> Unit
) : ListAdapter<Question, RecentQuestionsAdapter.ViewHolder>(QuestionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRecentQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemRecentQuestionBinding,
        private val onItemClick: (Question) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())

        fun bind(question: Question) {
            binding.apply {
                // 题目内容（取前50字符或识别文本）
                val content = question.notes?.take(50) ?: question.wrongAnswer?.take(50) ?: "无内容"
                textQuestionContent.text = content

                // 科目
                textSubject.text = question.subject

                // 错因标签
                textErrorReason.text = question.errorReason.toDisplayName()

                // 日期
                textDate.text = dateFormat.format(Date(question.errorDate))

                // 题目类型
                textQuestionType.text = question.questionType?.toDisplayName() ?: ""

                root.setOnClickListener {
                    onItemClick(question)
                }
            }
        }
    }

    class QuestionDiffCallback : DiffUtil.ItemCallback<Question>() {
        override fun areItemsTheSame(oldItem: Question, newItem: Question): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Question, newItem: Question): Boolean {
            return oldItem == newItem
        }
    }
}

fun ErrorReason.toDisplayName(): String = when (this) {
    ErrorReason.MISREAD -> "审题不清"
    ErrorReason.CALC_ERROR -> "计算错误"
    ErrorReason.CONCEPT_UNCLEAR -> "概念模糊"
    ErrorReason.KNOWLEDGE_GAP -> "知识点遗漏"
    ErrorReason.CARELESS -> "粗心大意"
    ErrorReason.OTHER -> "其他"
}

fun QuestionType.toDisplayName(): String = when (this) {
    QuestionType.CHOICE -> "选择题"
    QuestionType.FILL_BLANK -> "填空题"
    QuestionType.SOLUTION -> "解答题"
    QuestionType.PROOF -> "证明题"
    QuestionType.OTHER -> "其他"
}