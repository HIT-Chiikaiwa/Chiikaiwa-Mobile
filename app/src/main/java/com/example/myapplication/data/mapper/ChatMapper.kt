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

        val replyMsg = dto.replyToMessage?.let { ref ->
            Message(
                id = ref.id,
                conversationId = dto.conversationId ?: "",
                sender = User(id = "", fullName = ref.senderName ?: ""),
                content = ref.content ?: "",
                type = try { MessageType.valueOf(ref.messageType ?: "TEXT") } catch (e: Exception) { MessageType.TEXT }
            )
        }

        return Message(
            id = dto.id,
            conversationId = dto.conversationId ?: "",
            sender = sender,
            content = dto.content ?: "",
            type = msgType,
            status = MessageStatus.SENT,
            replyMessage = replyMsg,
            createdAt = dto.createdDate ?: "",
            updatedAt = dto.createdDate ?: "",
            isPinned = dto.isPinned,
            isRecalled = dto.isRecalled,
            attachments = dto.attachments.map { toDomain(it) },
            reactions = dto.reactions.map { toDomain(it) }
        )
    }

    fun toDomain(dto: AttachmentDto): Attachment {
        return Attachment(
            id = dto.id,
            url = dto.fileUrl,
            fileName = dto.fileName,
            fileSize = dto.fileSize,
            mimeType = dto.fileType
        )
    }

    fun toDomain(dto: ReactionDto): Reaction {
        return Reaction(
            emoji = dto.emoji,
            count = dto.count,
            userIds = dto.userIds
        )
    }
}
