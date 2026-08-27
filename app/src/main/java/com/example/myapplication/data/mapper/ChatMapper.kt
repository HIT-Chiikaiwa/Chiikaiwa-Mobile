package com.example.myapplication.data.mapper

import com.example.myapplication.data.model.*
import com.example.myapplication.data.remote.dto.response.*

object ChatMapper {
    fun toDomain(dto: ConversationResponse): Conversation {
        val convType = if (dto.type.equals("GROUP", ignoreCase = true)) ConversationType.GROUP else ConversationType.DIRECT
        return Conversation(
            id = dto.id,
            type = convType,
            name = dto.groupName ?: "",
            avatar = dto.groupAvatar,
            ownerId = null,
            lastMessage = dto.lastMessage?.let { toDomain(it) },
            lastMessageTime = dto.lastMessage?.createdDate ?: "",
            lastSenderId = dto.lastMessage?.senderId,
            memberCount = dto.memberCount,
            unreadCount = dto.unreadCount,
            hasLeft = dto.hasLeft
        )
    }

    fun toDomain(dto: MessageResponse): Message {
        val msgType = try {
            MessageType.valueOf(dto.messageType ?: "TEXT")
        } catch (e: Exception) {
            MessageType.TEXT
        }

        val sender = User(
            id = dto.senderId ?: "",
            fullName = dto.senderName ?: "",
            avatar = dto.senderAvatar
        )

        val replyTo = dto.replyToMessage?.let {
            ReplyMessage(
                id = it.id,
                senderName = it.senderName,
                content = it.content,
                messageType = it.messageType
            )
        }

        val reactionsList = dto.reactions?.map {
            Reaction(
                emoji = it.emoji,
                count = it.count,
                userIds = it.userIds ?: emptyList(),
                userNames = it.userNames ?: emptyList()
            )
        } ?: emptyList()

        return Message(
            id = dto.id,
            conversationId = dto.conversationId ?: "",
            sender = sender,
            content = dto.content ?: "",
            type = msgType,
            status = MessageStatus.SENT,
            createdAt = dto.createdDate ?: "",
            updatedAt = dto.createdDate ?: "",
            isRecalled = dto.isRecalled,
            replyToMessage = replyTo,
            reactions = reactionsList
        )
    }
}
