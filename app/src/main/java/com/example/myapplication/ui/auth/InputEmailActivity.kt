package com.example.myapplication.ui.auth

import android.content.Intent
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityInputEmailBinding
import com.example.myapplication.ui.base.BaseActivity

class InputEmailActivity : BaseActivity<ActivityInputEmailBinding>() {
    override fun inflateBinding() = ActivityInputEmailBinding.inflate(layoutInflater)

    private var email = ""

    override fun initView() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.tvLogin.setOnClickListener {
            email = binding.edtEmail.text.toString().trim()
            if (email.isBlank()) {
                showToast(getString(R.string.error_empty_email))
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast(getString(R.string.error_invalid_email))
            } else {
                val intent = Intent(this, VerifyEmailActivity::class.java).apply {
                    putExtra("email", email)
                    putExtra("flow", "forgot_password")
                }
                startActivity(intent)
            }
        }
    }

    override fun observeData() {
    }
}