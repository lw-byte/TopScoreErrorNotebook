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
import com.topscore.errornotebook.databinding.FragmentProfileBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeState()
        viewModel.onEvent(ProfileEvent.LoadProfile)
    }

    private fun setupClickListeners() {
        binding.btnSync.setOnClickListener {
            viewModel.onEvent(ProfileEvent.SyncNow)
        }

        binding.btnLogout.setOnClickListener {
            viewModel.onEvent(ProfileEvent.Logout)
        }

        binding.menuTags.setOnClickListener {
            // TODO: navigate to TagManagementFragment
        }

        binding.menuLearningStats.setOnClickListener {
            // TODO: navigate to learning stats
        }

        binding.menuAccountSecurity.setOnClickListener {
            // TODO: navigate to account security
        }

        binding.menuDarkMode.setOnClickListener {
            // TODO: toggle dark mode
        }

        binding.menuAboutUs.setOnClickListener {
            // TODO: show about us dialog
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    binding.tvUserName.text = state.user.name
                    binding.tvUserPhone.text = state.user.phone

                    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .withZone(ZoneId.systemDefault())
                    binding.tvLastSync.text = state.lastSyncTime?.let {
                        "Last sync: ${formatter.format(it)}"
                    } ?: "Never synced"

                    binding.syncProgress.visibility = if (state.syncStatus == SyncStatus.SYNCING) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    binding.btnSync.isEnabled = state.syncStatus != SyncStatus.SYNCING

                    binding.btnLogout.isEnabled = !state.isLoading
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = ProfileFragment()
    }
}