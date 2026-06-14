package com.topscore.errornotebook.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.topscore.errornotebook.databinding.FragmentTagManagementBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TagManagementFragment : Fragment() {

    private var _binding: FragmentTagManagementBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TagManagementViewModel by viewModels()

    private lateinit var tagAdapter: TagAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTagManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupClickListeners()
        observeState()
        viewModel.onEvent(TagManagementEvent.LoadTags)
    }

    private fun setupRecyclerView() {
        tagAdapter = TagAdapter { tag ->
            viewModel.onEvent(TagManagementEvent.DeleteTag(tag.id))
        }
        binding.rvTags.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tagAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddTag.setOnClickListener {
            val name = binding.etTagName.text?.toString()?.trim()
            if (!name.isNullOrEmpty()) {
                viewModel.onEvent(TagManagementEvent.CreateTag(name))
                binding.etTagName.text?.clear()
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    tagAdapter.submitList(state.tags)
                    binding.tvEmptyTags.visibility = if (state.tags.isEmpty() && !state.isLoading) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = TagManagementFragment()
    }
}