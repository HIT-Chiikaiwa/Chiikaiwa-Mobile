package com.example.myapplication.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentInputEmailBinding
import com.example.myapplication.ui.base.BaseFragment

class InputEmailFragment : BaseFragment<FragmentInputEmailBinding>() {

    override fun inflateBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentInputEmailBinding {
        return FragmentInputEmailBinding.inflate(inflater, container, false)
    }

    private var email = ""

    override fun initView() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnSendEmail.setOnClickListener {
            email = binding.edtEmail.text.toString().trim()
            if (email.isBlank()) {
                showToast(getString(R.string.error_empty_email))
            } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showToast(getString(R.string.error_invalid_email))
            } else {
                val bundle = Bundle().apply {
                    putString("email", email)
                    putString("flow", "forgot_password")
                }
                findNavController().navigate(
                    R.id.action_inputEmailFragment_to_verifyEmailFragment,
                    bundle
                )
            }
        }
    }

    override fun observeData() {
    }
}
