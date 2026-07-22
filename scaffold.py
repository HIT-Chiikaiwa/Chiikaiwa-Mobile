import os

base_pkg = "com.example.myapplication"
base_dir = r"e:\HIT_Product\app\src\main\java\com\example\myapplication"

files_to_create = {
    # Websocket
    r"data\remote\websocket\StompManager.kt": f"""package {base_pkg}.data.remote.websocket

class StompManager {{
    // TODO: Khởi tạo và quản lý kết nối STOMP
}}
""",
    r"data\remote\websocket\ChatSocketService.kt": f"""package {base_pkg}.data.remote.websocket

class ChatSocketService(private val stompManager: StompManager) {{
    // TODO: Giao tiếp WebSocket (subscribe, send messages)
}}
""",
    r"data\remote\websocket\SocketListener.kt": f"""package {base_pkg}.data.remote.websocket

interface SocketListener {{
    fun onConnected()
    fun onDisconnected()
    fun onMessageReceived(message: String)
}}
""",
    
    # Mapper
    r"data\mapper\MessageMapper.kt": f"""package {base_pkg}.data.mapper

import {base_pkg}.data.model.ChatMessage

object MessageMapper {{
    // fun mapToDomain(response: MessageResponse): ChatMessage {{ ... }}
}}
""",

    # Models (UI)
    r"data\model\ChatMessage.kt": f"""package {base_pkg}.data.model

data class ChatMessage(
    val id: String,
    val text: String,
    val senderId: String,
    val timestamp: Long
)
""",
    r"data\model\Friend.kt": f"""package {base_pkg}.data.model

data class Friend(
    val id: String,
    val name: String,
    val avatarUrl: String
)
""",

    # Repositories
    r"data\repository\ConversationRepository.kt": f"""package {base_pkg}.data.repository

import android.content.Context

class ConversationRepository(context: Context) : BaseRepository() {{
    // TODO: Lấy danh sách hội thoại từ Local/Remote
}}
""",
    r"data\repository\MessageRepository.kt": f"""package {base_pkg}.data.repository

import android.content.Context

class MessageRepository(context: Context) : BaseRepository() {{
    // TODO: Lấy tin nhắn, gửi tin nhắn
}}
""",
    r"data\repository\GroupRepository.kt": f"""package {base_pkg}.data.repository

import android.content.Context

class GroupRepository(context: Context) : BaseRepository() {{
    // TODO: Quản lý nhóm (thêm thành viên, xóa nhóm, v.v.)
}}
""",

    # UI - Chat - Conversation
    r"ui\home\chat\conversation\ConversationFragment.kt": f"""package {base_pkg}.ui.home.chat.conversation

import androidx.fragment.app.Fragment

class ConversationFragment : Fragment() {{
    // TODO: Hiển thị danh sách hội thoại
}}
""",
    r"ui\home\chat\conversation\ConversationViewModel.kt": f"""package {base_pkg}.ui.home.chat.conversation

import {base_pkg}.ui.base.BaseViewModel

class ConversationViewModel : BaseViewModel() {{
    // TODO: Xử lý logic cho ConversationFragment
}}
""",

    # UI - Chat - ChatRoom
    r"ui\home\chat\chatroom\ChatFragment.kt": f"""package {base_pkg}.ui.home.chat.chatroom

import androidx.fragment.app.Fragment

class ChatFragment : Fragment() {{
    // TODO: Màn hình nhắn tin chính
}}
""",
    r"ui\home\chat\chatroom\ChatViewModel.kt": f"""package {base_pkg}.ui.home.chat.chatroom

import {base_pkg}.ui.base.BaseViewModel

class ChatViewModel : BaseViewModel() {{
    // TODO: Xử lý gửi, nhận, load tin nhắn
}}
""",

    # UI - Chat - Group
    r"ui\home\chat\group\GroupDetailFragment.kt": f"""package {base_pkg}.ui.home.chat.group

import androidx.fragment.app.Fragment

class GroupDetailFragment : Fragment() {{
    // TODO: Màn hình thông tin nhóm
}}
""",
    r"ui\home\chat\group\GroupViewModel.kt": f"""package {base_pkg}.ui.home.chat.group

import {base_pkg}.ui.base.BaseViewModel

class GroupViewModel : BaseViewModel() {{
    // TODO: Xử lý logic nhóm
}}
""",

    # UI - Chat - Adapter
    r"ui\home\chat\adapter\MessageAdapter.kt": f"""package {base_pkg}.ui.home.chat.adapter

// TODO: RecyclerView Adapter với Multiple ViewTypes (Incoming, Outgoing)
class MessageAdapter {{
}}
""",
    r"ui\home\chat\adapter\viewholder\IncomingTextViewHolder.kt": f"""package {base_pkg}.ui.home.chat.adapter.viewholder

// TODO: ViewHolder cho tin nhắn đến
class IncomingTextViewHolder {{
}}
""",
    r"ui\home\chat\adapter\viewholder\OutgoingTextViewHolder.kt": f"""package {base_pkg}.ui.home.chat.adapter.viewholder

// TODO: ViewHolder cho tin nhắn đi
class OutgoingTextViewHolder {{
}}
""",

    # UI - Chat - Component
    r"ui\home\chat\component\ReactionPopup.kt": f"""package {base_pkg}.ui.home.chat.component

class ReactionPopup {{
    // TODO: Popup thả cảm xúc
}}
"""
}

for rel_path, content in files_to_create.items():
    full_path = os.path.join(base_dir, rel_path)
    os.makedirs(os.path.dirname(full_path), exist_ok=True)
    with open(full_path, "w", encoding="utf-8") as f:
        f.write(content)
    print(f"Created {rel_path}")
