package com.topscore.errornotebook.ui.question

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.topscore.errornotebook.R
import com.topscore.errornotebook.databinding.FragmentQuestionListBinding
import com.topscore.errornotebook.domain.model.ErrorReason
import com.topscore.errornotebook.domain.model.SubjectStage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Question List Fragment
 * Displays a list of questions with filtering, search, and batch selection capabilities
 */
@AndroidEntryPoint
class QuestionListFragment : Fragment() {

    private var _binding: FragmentQuestionListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuestionListViewModel by viewModels()
    private lateinit var questionAdapter: QuestionAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchBar()
        setupFilterChips()
        setupSelectionMode()
        observeState()
    }

    private fun setupRecyclerView() {
        questionAdapter = QuestionAdapter(
            onItemClick = { question ->
                if (viewModel.uiState.value.isSelectionMode) {
                    viewModel.onEvent(QuestionListEvent.ToggleSelection(question.id))
                } else {
                    navigateToDetail(question.id)
                }
            },
            onItemLongClick = { question ->
                viewModel.onEvent(QuestionListEvent.ToggleSelection(question.id))
            }
        )
        binding.recyclerView.apply {
            adapter = questionAdapter
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
        }
    }

    private fun setupSearchBar() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                viewModel.onEvent(QuestionListEvent.Search(query ?: ""))
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.onEvent(QuestionListEvent.Search(newText ?: ""))
                return true
            }
        })
    }

    private fun setupFilterChips() {
        // Stage filter chips
        binding.chipStageAll.isChecked = true
        binding.chipStagePrimary.setOnClickListener { applyStageFilter(SubjectStage.PRIMARY) }
        binding.chipStageMiddle.setOnClickListener { applyStageFilter(SubjectStage.MIDDLE) }
        binding.chipStageHigh.setOnClickListener { applyStageFilter(SubjectStage.HIGH) }
        binding.chipStageAll.setOnClickListener { applyStageFilter(null) }

        // Error reason filter chips
        binding.chipReasonAll.isChecked = true
        ErrorReason.entries.forEach { reason ->
            val chipId = getReasonChipId(reason)
            binding.chipGroupReason.findViewById<Chip>(chipId)?.setOnClickListener {
                applyErrorReasonFilter(reason)
            }
        }
        binding.chipReasonAll.setOnClickListener { applyErrorReasonFilter(null) }

        // Subject filter chips
        binding.chipSubjectAll.isChecked = true
        binding.chipSubjectMath.setOnClickListener { applySubjectFilter("数学") }
        binding.chipSubjectChinese.setOnClickListener { applySubjectFilter("语文") }
        binding.chipSubjectEnglish.setOnClickListener { applySubjectFilter("英语") }
        binding.chipSubjectPhysics.setOnClickListener { applySubjectFilter("物理") }
        binding.chipSubjectChemistry.setOnClickListener { applySubjectFilter("化学") }
        binding.chipSubjectBiology.setOnClickListener { applySubjectFilter("生物") }
        binding.chipSubjectPolitics.setOnClickListener { applySubjectFilter("政治") }
        binding.chipSubjectHistory.setOnClickListener { applySubjectFilter("历史") }
        binding.chipSubjectGeography.setOnClickListener { applySubjectFilter("地理") }
        binding.chipSubjectAll.setOnClickListener { applySubjectFilter(null) }
    }

    private fun applyStageFilter(stage: SubjectStage?) {
        val currentFilter = viewModel.uiState.value.filter
        viewModel.onEvent(QuestionListEvent.ApplyFilter(currentFilter.copy(stage = stage)))
    }

    private fun applyErrorReasonFilter(reason: ErrorReason?) {
        val currentFilter = viewModel.uiState.value.filter
        val reasons = if (reason != null) listOf(reason) else null
        viewModel.onEvent(QuestionListEvent.ApplyFilter(currentFilter.copy(errorReasons = reasons)))
    }

    private fun applySubjectFilter(subject: String?) {
        val currentFilter = viewModel.uiState.value.filter
        viewModel.onEvent(QuestionListEvent.ApplyFilter(currentFilter.copy(subject = subject)))
    }

    private fun getReasonChipId(reason: ErrorReason): Int {
        return when (reason) {
            ErrorReason.MISREAD -> R.id.chip_reason_misread
            ErrorReason.CALC_ERROR -> R.id.chip_reason_calc
            ErrorReason.CONCEPT_UNCLEAR -> R.id.chip_reason_concept
            ErrorReason.KNOWLEDGE_GAP -> R.id.chip_reason_knowledge
            ErrorReason.CARELESS -> R.id.chip_reason_careless
            ErrorReason.OTHER -> R.id.chip_reason_other
        }
    }

    private fun setupSelectionMode() {
        binding.btnDelete.setOnClickListener {
            viewModel.onEvent(QuestionListEvent.DeleteSelected)
        }

        binding.btnCancel.setOnClickListener {
            viewModel.onEvent(QuestionListEvent.ClearSelection)
        }

        binding.fabExport.setOnClickListener {
            navigateToExport()
        }
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

    private fun updateUi(state: QuestionListUiState) {
        binding.progressBar.isVisible = state.isLoading
        binding.emptyState.isVisible = !state.isLoading && state.filteredQuestions.isEmpty()
        binding.recyclerView.isVisible = !state.isLoading && state.filteredQuestions.isNotEmpty()

        questionAdapter.submitList(state.filteredQuestions, state.selectedIds, state.isSelectionMode)

        // Update selection mode UI
        binding.selectionToolbar.isVisible = state.isSelectionMode
        binding.filterSection.isVisible = !state.isSelectionMode

        if (state.isSelectionMode) {
            binding.tvSelectedCount.text = getString(R.string.selected_count, state.selectedIds.size)
        }

        // Show export FAB when there are filtered questions and not in selection mode
        binding.fabExport.isVisible = !state.isSelectionMode && state.filteredQuestions.isNotEmpty()
    }

    private fun handleEvent(event: QuestionListEvent?) {
        when (event) {
            is QuestionListEvent.ShowMessage -> {
                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                viewModel.onEvent(QuestionListEvent.ConsumeEvent)
            }
            is QuestionListEvent.ShowError -> {
                Toast.makeText(requireContext(), event.error, Toast.LENGTH_SHORT).show()
                viewModel.onEvent(QuestionListEvent.ConsumeEvent)
            }
            else -> {}
        }
    }

    private fun navigateToDetail(questionId: Long) {
        val action = QuestionListFragmentDirections
            .actionQuestionListToQuestionDetail(questionId)
        findNavController().navigate(action)
    }

    private fun navigateToExport() {
        val questionIds = viewModel.uiState.value.filteredQuestions.map { it.id }.toLongArray()
        val action = QuestionListFragmentDirections
            .actionQuestionListToExport(questionIds)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}