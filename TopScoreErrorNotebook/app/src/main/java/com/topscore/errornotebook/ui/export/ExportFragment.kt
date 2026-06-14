package com.topscore.errornotebook.ui.export

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.topscore.errornotebook.R
import com.topscore.errornotebook.databinding.FragmentExportBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ExportFragment : Fragment() {

    private var _binding: FragmentExportBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ExportViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeStateFlow()
        viewModel.onEvent(ExportEvent.LoadQuestions)
    }

    private fun setupViews() {
        // 打印内容 - 复选框
        binding.checkQuestion.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(includeQuestion = isChecked) }
        }

        binding.checkAnswer.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(includeAnswer = isChecked) }
        }

        binding.checkNotes.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(includeNotes = isChecked) }
        }

        binding.checkSource.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(includeSource = isChecked) }
        }

        binding.checkTags.setOnCheckedChangeListener { _, isChecked ->
            updateConfig { it.copy(includeTags = isChecked) }
        }

        // 答案放置 - 单选按钮
        binding.radioSeparatePage.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateConfig { it.copy(answerPlacement = AnswerPlacement.SEPARATE_PAGE) }
            }
        }

        binding.radioInterleaved.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateConfig { it.copy(answerPlacement = AnswerPlacement.INTERLEAVED) }
            }
        }

        // 纸张方向 - 单选按钮
        binding.radioPortrait.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateConfig { it.copy(pageOrientation = PageOrientation.PORTRAIT) }
            }
        }

        binding.radioLandscape.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateConfig { it.copy(pageOrientation = PageOrientation.LANDSCAPE) }
            }
        }

        // 题目间留白 - 滑动条
        binding.sliderSpacing.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                updateConfig { it.copy(spacing = value.toInt()) }
                binding.textSpacingValue.text = "${value.toInt()}dp"
            }
        }

        // 字体大小 - 单选按钮组
        binding.radioFontSmall.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateConfig { it.copy(fontSize = FontSize.SMALL) }
            }
        }

        binding.radioFontMedium.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateConfig { it.copy(fontSize = FontSize.MEDIUM) }
            }
        }

        binding.radioFontLarge.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                updateConfig { it.copy(fontSize = FontSize.LARGE) }
            }
        }

        // 打印预览按钮
        binding.btnPreview.setOnClickListener {
            viewModel.onEvent(ExportEvent.Preview)
        }

        // 导出 Word 按钮
        binding.btnExportWord.setOnClickListener {
            viewModel.onEvent(ExportEvent.ExportToWord)
        }

        // 导出 PDF 按钮
        binding.btnExportPdf.setOnClickListener {
            viewModel.onEvent(ExportEvent.ExportToPdf)
        }

        // 直接打印按钮
        binding.btnPrint.setOnClickListener {
            viewModel.onEvent(ExportEvent.Print)
        }
    }

    private fun updateConfig(update: (ExportConfig) -> ExportConfig) {
        val currentConfig = viewModel.uiState.value.config
        viewModel.onEvent(ExportEvent.UpdateConfig(update(currentConfig)))
    }

    private fun observeStateFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        updateUi(state)
                    }
                }
                launch {
                    viewModel.navigationEvent.collect { event ->
                        handleNavigationEvent(event)
                    }
                }
            }
        }
    }

    private fun updateUi(state: ExportUiState) {
        // 更新加载状态
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.contentLayout.visibility = if (state.isLoading) View.GONE else View.VISIBLE

        // 更新选中数量
        binding.textSelectedCount.text = getString(
            R.string.selected_count,
            state.selectedQuestions.size
        )

        // 更新配置 UI
        val config = state.config

        // 打印内容复选框
        binding.checkQuestion.isChecked = config.includeQuestion
        binding.checkAnswer.isChecked = config.includeAnswer
        binding.checkNotes.isChecked = config.includeNotes
        binding.checkSource.isChecked = config.includeSource
        binding.checkTags.isChecked = config.includeTags

        // 答案放置
        when (config.answerPlacement) {
            AnswerPlacement.SEPARATE_PAGE -> binding.radioSeparatePage.isChecked = true
            AnswerPlacement.INTERLEAVED -> binding.radioInterleaved.isChecked = true
        }

        // 纸张方向
        when (config.pageOrientation) {
            PageOrientation.PORTRAIT -> binding.radioPortrait.isChecked = true
            PageOrientation.LANDSCAPE -> binding.radioLandscape.isChecked = true
        }

        // 题目间留白
        binding.sliderSpacing.value = config.spacing.toFloat()
        binding.textSpacingValue.text = "${config.spacing}dp"

        // 字体大小
        when (config.fontSize) {
            FontSize.SMALL -> binding.radioFontSmall.isChecked = true
            FontSize.MEDIUM -> binding.radioFontMedium.isChecked = true
            FontSize.LARGE -> binding.radioFontLarge.isChecked = true
        }

        // 更新导出按钮状态
        binding.btnExportWord.isEnabled = !state.isExporting && state.selectedQuestions.isNotEmpty()
        binding.btnExportPdf.isEnabled = !state.isExporting && state.selectedQuestions.isNotEmpty()
        binding.btnPrint.isEnabled = !state.isExporting && state.selectedQuestions.isNotEmpty()
        binding.btnPreview.isEnabled = !state.isExporting && state.selectedQuestions.isNotEmpty()

        // 显示错误信息
        state.errorMessage?.let {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleNavigationEvent(event: ExportEvent) {
        when (event) {
            is ExportEvent.NavigateToPreview -> {
                // TODO: 跳转到预览页面
                Toast.makeText(requireContext(), "预览功能开发中", Toast.LENGTH_SHORT).show()
            }
            is ExportEvent.ExportComplete -> {
                Toast.makeText(requireContext(), "导出成功", Toast.LENGTH_SHORT).show()
            }
            is ExportEvent.ShowError -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
            }
            else -> { /* handled in state flow */ }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}