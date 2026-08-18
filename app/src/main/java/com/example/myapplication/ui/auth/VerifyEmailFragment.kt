package com.example.myapplication.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentVerifyEmailBinding
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState

class VerifyEmailFragment : BaseFragment<FragmentVerifyEmailBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentVerifyEmailBinding {
        return FragmentVerifyEmailBinding.inflate(inflater, container, false)
    }

    private val viewModel: VerifyEmailViewModel by viewModels()

    private val email: String by lazy { arguments?.getString("email").orEmpty() }
    private val flow: String by lazy { arguments?.getString("flow") ?: "register" }

    override fun initView() {
        binding.tvDescription.text = getString(R.string.verify_your_email_format, email)

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSendEmail.setOnClickListener {
            startActivity(
                Intent(requireContext(), VerifyOtpActivity::class.java).apply {
                    putExtra("email", email)
                    putExtra("flow", flow)
                }
            )
        }
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                UiState.Idle -> {
                    setLoading(false)
                }
                UiState.Loading -> {
                    setLoading(true)
                }
                is UiState.Success -> {
                    setLoading(false)
                    viewModel.resetState()
                    startActivity(
                        Intent(requireContext(), VerifyOtpActivity::class.java).apply {
                            putExtra("email", email)
                            putExtra("flow", flow)
                        }
                    )
                }
                is UiState.Error -> {
                    setLoading(false)
                    showToast(state.message)
                }
            }
        }

        viewModel.event.observeEvent { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    showToast(event.message)
                }
                else -> {}
            }
        }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSendEmail.isEnabled = !isLoading
        binding.btnSendEmail.alpha = if (isLoading) 0.5f else 1.0f
    }
}
