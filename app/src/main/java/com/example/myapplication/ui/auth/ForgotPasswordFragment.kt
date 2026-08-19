package com.example.myapplication.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentForgotPasswordBinding
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState

class ForgotPasswordFragment : BaseFragment<FragmentForgotPasswordBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentForgotPasswordBinding {
        return FragmentForgotPasswordBinding.inflate(inflater, container, false)
    }

    private val viewModel: ForgotPasswordViewModel by viewModels()

    private val email: String by lazy { arguments?.getString("email").orEmpty() }
    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun initView() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.ivTogglePassword.setOnClickListener {
            togglePasswordVisibility()
        }

        binding.ivToggleConfirmPassword.setOnClickListener {
            toggleConfirmPasswordVisibility()
        }

        binding.tvConfirm.setOnClickListener {
            binding.tvPasswordError.visibility = View.GONE
            viewModel.resetPassword(
                email = email,
                pass = binding.edtNewPassword.text.toString().trim(),
                confirmPass = binding.edtConfirmPassword.text.toString().trim()
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
                    showToast(state.data)
                    findNavController().popBackStack(R.id.loginFragment, false)
                }
                is UiState.Error -> {
                    setLoading(false)
                    binding.tvPasswordError.visibility = View.VISIBLE
                    binding.tvPasswordError.text = state.message
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
        binding.tvConfirm.isEnabled = !isLoading
        binding.tvConfirm.alpha = if (isLoading) 0.5f else 1.0f
        if (isLoading) {
            binding.tvPasswordError.visibility = View.GONE
        }
    }

    private fun togglePasswordVisibility() {
        if (isPasswordVisible) {
            binding.edtNewPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
        } else {
            binding.edtNewPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility)
        }
        binding.edtNewPassword.setSelection(binding.edtNewPassword.text.length)
        isPasswordVisible = !isPasswordVisible
        updateAvatarState()
    }

    private fun toggleConfirmPasswordVisibility() {
        if (isConfirmPasswordVisible) {
            binding.edtConfirmPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility_off)
        } else {
            binding.edtConfirmPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.ivToggleConfirmPassword.setImageResource(R.drawable.ic_visibility)
        }
        binding.edtConfirmPassword.setSelection(binding.edtConfirmPassword.text.length)
        isConfirmPasswordVisible = !isConfirmPasswordVisible
        updateAvatarState()
    }

    private fun updateAvatarState() {
        if (isPasswordVisible || isConfirmPasswordVisible) {
            binding.ivPic.setImageResource(R.drawable.frame1)
        } else {
            binding.ivPic.setImageResource(R.drawable.frame3)
        }
    }
}
