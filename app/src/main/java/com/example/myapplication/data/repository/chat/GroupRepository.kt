package com.example.myapplication.data.repository.chat
import com.example.myapplication.data.repository.BaseRepository

import android.content.Context
import com.example.myapplication.data.remote.network.RetrofitClient

class GroupRepository(context: Context) : BaseRepository() {
    private val api = RetrofitClient.create(context)
}
