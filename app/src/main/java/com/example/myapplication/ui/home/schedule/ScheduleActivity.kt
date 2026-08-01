package com.example.myapplication.ui.home.schedule

import com.example.myapplication.databinding.FragmentScheduleBinding
import com.example.myapplication.ui.base.BaseActivity

class ScheduleActivity : BaseActivity<FragmentScheduleBinding>() {

    override fun inflateBinding() = FragmentScheduleBinding.inflate(layoutInflater)

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    override fun observeData() {
    }
}
