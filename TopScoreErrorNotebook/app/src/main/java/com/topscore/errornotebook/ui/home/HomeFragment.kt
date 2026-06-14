package com.topscore.errornotebook.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.topscore.errornotebook.R
import com.topscore.errornotebook.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var recentQuestionsAdapter: RecentQuestionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeStateFlow()
        viewModel.onEvent(HomeEvent.LoadHomeData)
    }

    private fun setupViews() {
        // 设置最近问题列表
        recentQuestionsAdapter = RecentQuestionsAdapter { question ->
            viewModel.onEvent(HomeEvent.NavigateToQuestionDetail(question.id))
        }
        binding.recyclerRecentQuestions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recentQuestionsAdapter
        }
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

    private fun updateUi(state: HomeUiState) {
        // 更新加载状态
        binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.contentLayout.visibility = if (state.isLoading) View.GONE else View.VISIBLE

        // 更新欢迎语
        binding.textWelcome.text = getString(R.string.home_welcome, state.userName)

        // 更新统计卡片
        binding.textErrorCount.text = state.errorCount.toString()
        binding.textInProgressCount.text = state.inProgressCount.toString()
        binding.textMasteredCount.text = state.masteredCount.toString()
        binding.textTodayCount.text = state.todayCount.toString()

        // 更新最近问题列表
        if (state.recentQuestions.isEmpty()) {
            binding.recyclerRecentQuestions.visibility = View.GONE
            binding.textEmpty.visibility = View.VISIBLE
        } else {
            binding.recyclerRecentQuestions.visibility = View.VISIBLE
            binding.textEmpty.visibility = View.GONE
            recentQuestionsAdapter.submitList(state.recentQuestions)
        }
    }

    private fun handleNavigationEvent(event: HomeEvent.NavigateToQuestionDetail) {
        val action = HomeFragmentDirections.actionHomeToQuestionDetail(event.questionId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}