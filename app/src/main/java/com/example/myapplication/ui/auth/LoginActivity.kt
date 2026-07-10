package com.example.myapplication.ui.auth

import android.content.Intent
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import androidx.activity.viewModels
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.ui.base.BaseActivity
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState

class LoginActivity : BaseActivity<ActivityLoginBinding>() {

    override fun inflateBinding() = ActivityLoginBinding.inflate(layoutInflater)

    private val viewModel: LoginViewModel by viewModels()

    private var isPasswordVisible = false

    override fun initView() {
        binding.tvLogin.setOnClickListener {
            binding.tvPasswordError.visibility = View.GONE

            viewModel.login(
                binding.edtEmail.text.toString().trim(),
                binding.edtPassword.text.toString().trim()
            )
        }

        binding.ivTogglePassword.setOnClickListener {
            togglePassword()
        }

        binding.tvForgot.setOnClickListener {
            startActivity(
                Intent(this, InputEmailActivity::class.java)
            )
        }

        binding.tvRegister.setOnClickListener {
            startActivity(
                Intent(this, RegisterActivity::class.java)
            )
        }
    }

    override fun observeData() {
        viewModel.uiState.observe(this) { state ->

            when (state) {

                UiState.Idle -> {
                    binding.tvLogin.isEnabled = true
                }

                UiState.Loading -> {
                    binding.tvLogin.isEnabled = false
                }

                is UiState.Success -> {
                    binding.tvLogin.isEnabled = true
                }

                is UiState.Error -> {

                    binding.tvLogin.isEnabled = true

                    binding.tvPasswordError.visibility = View.VISIBLE
                    binding.tvPasswordError.text = state.message

                    binding.ivPic.setImageResource(R.drawable.frame2)
                }
            }
        }

        viewModel.event.observe(this) { event ->

            when (event) {

                is UiEvent.ShowToast -> {
                    showToast(event.message)
                }

                UiEvent.NavigateHome -> {
                    startActivity(
                        Intent(this, com.example.myapplication.ui.profile.ProfileActivity::class.java)
                    )
                    finish()
                }
            }
        }
    }

    private fun togglePassword() {
        val editText = binding.edtPassword

        if (isPasswordVisible) {
            editText.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            binding.ivPic.setImageResource(R.drawable.frame3)

        } else {
            editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            binding.ivPic.setImageResource(R.drawable.frame1)
        }


        isPasswordVisible = !isPasswordVisible
        editText.setSelection(editText.text.length)
    }
}