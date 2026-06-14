package com.topscore.errornotebook.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import com.topscore.errornotebook.R
import com.topscore.errornotebook.databinding.FragmentLoginBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeState()
        observeEffects()
    }

    private fun setupViews() {
        binding.apply {
            // Phone input
            etPhone.doAfterTextChanged { text ->
                viewModel.onEvent(AuthEvent.PhoneChanged(text?.toString() ?: ""))
            }

            // Code input
            etCode.doAfterTextChanged { text ->
                viewModel.onEvent(AuthEvent.CodeChanged(text?.toString() ?: ""))
            }

            // Get code button
            btnGetCode.setOnClickListener {
                viewModel.onEvent(AuthEvent.SendSmsCode)
            }

            // Login button
            btnLogin.setOnClickListener {
                viewModel.onEvent(AuthEvent.LoginWithCode)
            }
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    updateUi(state)
                }
            }
        }
    }

    private fun updateUi(state: AuthUiState) {
        binding.apply {
            // Update get code button
            if (state.countdown > 0) {
                btnGetCode.isEnabled = false
                btnGetCode.text = getString(R.string.countdown_format, state.countdown)
            } else {
                btnGetCode.isEnabled = true
                btnGetCode.text = getString(R.string.get_code)
            }

            // Update login button
            btnLogin.isEnabled = !state.isLoading

            // Show error
            state.errorMessage?.let { message ->
                tilPhone.error = if (message.contains("手机号")) message else null
                tilCode.error = if (message.contains("验证码")) message else null

                if (message.contains("手机号") || message.contains("验证码")) {
                    Snackbar.make(root, message, Snackbar.LENGTH_SHORT).show()
                }
            } ?: run {
                tilPhone.error = null
                tilCode.error = null
            }
        }
    }

    private fun observeEffects() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.effect.collect { effect ->
                    effect?.let {
                        handleEffect(it)
                        viewModel.consumeEffect()
                    }
                }
            }
        }
    }

    private fun handleEffect(effect: AuthEffect) {
        when (effect) {
            is AuthEffect.SmsCodeSent -> {
                Snackbar.make(
                    binding.root,
                    R.string.sms_code_sent,
                    Snackbar.LENGTH_SHORT
                ).show()
            }
            is AuthEffect.LoginSuccess -> {
                Snackbar.make(
                    binding.root,
                    R.string.login_success,
                    Snackbar.LENGTH_SHORT
                ).show()
                // Navigate to main or home
            }
            is AuthEffect.ShowError -> {
                Snackbar.make(binding.root, effect.message, Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
