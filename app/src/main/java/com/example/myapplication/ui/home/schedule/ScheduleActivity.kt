package com.example.myapplication.ui.home.schedule

import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.databinding.FragmentScheduleBinding
import com.example.myapplication.ui.base.BaseActivity

class ScheduleActivity : BaseActivity<FragmentScheduleBinding>() {

    override fun inflateBinding() = FragmentScheduleBinding.inflate(layoutInflater)

    override fun initView() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        binding.rvScheduleList.layoutManager = LinearLayoutManager(this)
        binding.btnCreateSchedule.setOnClickListener {
            startActivity(android.content.Intent(this, CreateAppointmentActivity::class.java))
        }
    }

    override fun observeData() {
    }
}
