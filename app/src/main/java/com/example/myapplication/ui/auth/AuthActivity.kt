package com.example.myapplication.ui.auth

import com.example.myapplication.databinding.ActivityAuthBinding
import com.example.myapplication.ui.base.BaseActivity

class AuthActivity : BaseActivity<ActivityAuthBinding>() {

    override fun inflateBinding(): ActivityAuthBinding {
        return ActivityAuthBinding.inflate(layoutInflater)
    }

    override fun initView() { 
    }

    override fun observeData() {
    }
}
