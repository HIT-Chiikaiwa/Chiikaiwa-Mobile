package com.example.myapplication.ui.auth

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentLoginBinding
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginFragment : BaseFragment<FragmentLoginBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentLoginBinding {
        return FragmentLoginBinding.inflate(inflater, container, false)
    }

    private val viewModel: LoginViewModel by viewModels()

    private var isPasswordVisible = false
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken
                val email = account?.email
                if (idToken != null) {
                    viewModel.googleLogin(idToken, email)
                } else {
                    showToast("Không nhận được token từ Google")
                }
            } catch (e: ApiException) {
                android.util.Log.e("GOOGLE_AUTH", "Google Sign-In failed: ${e.statusCode}")
                showToast("Đăng nhập Google thất bại (Mã lỗi: ${e.statusCode})")
            }
        } else {
            showToast("Hủy đăng nhập Google")
        }
    }

    private fun initGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
    }

    override fun initView() {
        initGoogleSignIn()

        binding.tvLogin.setOnClickListener {
            binding.tvPasswordError.visibility = View.GONE

            viewModel.login(
                binding.edtEmail.text.toString().trim(),
                binding.edtPassword.text.toString().trim()
            )
        }

        binding.btnGoogleLogin.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }

        binding.ivTogglePassword.setOnClickListener {
            togglePassword()
        }

        binding.tvForgot.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_inputEmailFragment)
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                UiState.Idle -> {
                    binding.tvLogin.isEnabled = true
                    binding.btnGoogleLogin.isEnabled = true
                }

                UiState.Loading -> {
                    binding.tvLogin.isEnabled = false
                    binding.btnGoogleLogin.isEnabled = false
                }

                is UiState.Success -> {
                    binding.tvLogin.isEnabled = true
                    binding.btnGoogleLogin.isEnabled = true
                }

                is UiState.Error -> {
                    binding.tvLogin.isEnabled = true
                    binding.btnGoogleLogin.isEnabled = true
                    binding.tvPasswordError.visibility = View.VISIBLE
                    binding.tvPasswordError.text = state.message
                    binding.ivPic.setImageResource(R.drawable.frame2)
                }
            }
        }

        viewModel.event.observeEvent { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    showToast(event.message)
                }

                UiEvent.NavigateHome -> {
                    startActivity(
                        Intent(requireContext(), com.example.myapplication.ui.main.MainActivity::class.java)
                    )
                    requireActivity().finish()
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
