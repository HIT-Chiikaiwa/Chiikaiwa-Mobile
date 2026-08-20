package com.example.myapplication.ui.profile

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.myapplication.R
import com.example.myapplication.data.remote.dto.response.UserDto
import com.example.myapplication.databinding.FragmentAccountSettingsBinding
import com.example.myapplication.ui.auth.AuthActivity
import com.example.myapplication.ui.base.BaseFragment
import com.example.myapplication.ui.base.UiEvent
import com.example.myapplication.ui.base.UiState

class AccountSettingsFragment : BaseFragment<FragmentAccountSettingsBinding>() {

    override fun inflateBinding(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentAccountSettingsBinding.inflate(inflater, container, false)

    private val viewModel: ProfileViewModel by viewModels()
    private var currentUser: UserDto? = null

    override fun initView() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.switchRadar.setOnCheckedChangeListener { _, isChecked ->
            val user = currentUser ?: return@setOnCheckedChangeListener
            if (isChecked != (user.buddyActive == true)) {
                viewModel.toggleBuddyStatus(isChecked)
            }
        }

        binding.cardPersonalInfo.setOnClickListener {
            findNavController().navigate(R.id.action_accountSettingsFragment_to_registrationInfoFragment)
        }

        binding.cardChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_accountSettingsFragment_to_changePasswordFragment)
        }

        binding.cardNotificationSettings.setOnClickListener {
            findNavController().navigate(R.id.action_accountSettingsFragment_to_notificationSettingsFragment)
        }

        binding.cardBlockedUsers.setOnClickListener {
            findNavController().navigate(R.id.action_accountSettingsFragment_to_blockedUsersFragment)
        }

        binding.cardHelp.setOnClickListener {
            showHelpDialog()
        }

        binding.cardLogout.setOnClickListener {
            viewModel.logout()
        }

        viewModel.loadProfile()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadProfile()
    }

    override fun observeData() {
        viewModel.uiState.observeState { state ->
            when (state) {
                is UiState.Success -> {
                    currentUser = state.data
                    binding.switchRadar.isChecked = state.data.buddyActive == true
                }
                is UiState.Error -> showToast(state.message)
                else -> {}
            }
        }

        viewModel.event.observeEvent { event ->
            when (event) {
                is UiEvent.ShowToast -> showToast(event.message)
                is UiEvent.NavigateHome -> {
                    val intent = Intent(requireContext(), AuthActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }
            }
        }
    }

    private fun showHelpDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Trợ giúp & Hỗ trợ")
            .setMessage("Mọi thắc mắc hoặc cần hỗ trợ kỹ thuật, vui lòng liên hệ nhóm phát triển qua email support@studydate.com.")
            .setPositiveButton("Đóng", null)
            .show()
    }
}
