package com.example.myapplication.ui.home.chat.group

import android.app.Application
import com.example.myapplication.data.model.Conversation
import com.example.myapplication.data.repository.GroupRepository
import com.example.myapplication.ui.base.BaseViewModel

class GroupViewModel(application: Application) : BaseViewModel<Conversation>(application) {

    private val repository = GroupRepository(application)
}
