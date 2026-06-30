package com.example.myapplication.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityLoginBinding
import com.example.myapplication.utils.PreferenceManager
import kotlin.jvm.java

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    private lateinit var viewModel: LoginViewModel

    private lateinit var preferenceManager: PreferenceManager

    private var isPasswordVisible = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
        preferenceManager = PreferenceManager(this)

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        initView()

        observeViewModel()
    }

    private fun initView() {
        binding.tvLogin.setOnClickListener {
            login()
        }

        binding.ivTogglePassword.setOnClickListener {
            togglePassword()
        }
    }

    private fun login() {
        val email = binding.edtEmail.text.toString().trim()

        val password = binding.edtPassword.text.toString().trim()

        if (email.isEmpty()) {
            binding.edtEmail.error = "Nhập email"
            return
        }

        if (password.isEmpty()) {
            binding.edtPassword.error = "Nhập mật khẩu"
            return
        }

        binding.tvPasswordError.visibility = View.GONE
        viewModel.login(email, password)

    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { response ->
            response?.let {
                preferenceManager.saveLogin(
                    accessToken = it.accessToken,
                    refreshToken = it.refreshToken,
                    userId = it.id
                )
                Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                startActivity(
                    Intent(this, LoginActivity::class.java)
                )
                finish()
            }

        }

        viewModel.error.observe(this) { message ->
            binding.tvPasswordError.visibility = View.VISIBLE
            binding.tvPasswordError.text = message
            binding.ivPic.setImageResource(R.drawable.frame2)
        }

    }

    private fun togglePassword() {
        if (isPasswordVisible) {
            binding.edtPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility)
            binding.ivPic.setImageResource(R.drawable.frame1)
        } else {
            binding.edtPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            binding.ivTogglePassword.setImageResource(R.drawable.ic_visibility_off)
            binding.ivPic.setImageResource(R.drawable.frame3)
        }

        binding.edtPassword.setSelection(binding.edtPassword.text.length)
        isPasswordVisible = !isPasswordVisible
    }
}