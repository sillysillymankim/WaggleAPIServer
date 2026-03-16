package io.waggle.waggleapiserver.domain.conversation.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.domain.conversation.Conversation
import io.waggle.waggleapiserver.domain.conversation.dto.response.ConversationResponse
import io.waggle.waggleapiserver.domain.conversation.repository.ConversationRepository
import io.waggle.waggleapiserver.domain.message.Message
import io.waggle.waggleapiserver.domain.message.repository.MessageRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class ConversationService(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun getConversations(
        cursorQuery: CursorGetQuery,
        user: User,
    ): CursorResponse<ConversationResponse> {
        val (cursor, size) = cursorQuery
        val pageable = PageRequest.of(0, size + 1)

        val conversations = conversationRepository.findByUserId(user.id, cursor, pageable)

        val hasNext = conversations.size > size
        val trimmed = if (hasNext) conversations.dropLast(1) else conversations

        val partnerIds = trimmed.map { it.partnerId }
        val partners = userRepository.findAllById(partnerIds).associateBy { it.id }

        val lastMessageIds = trimmed.map { it.lastMessageId }
        val messages = messageRepository.findAllById(lastMessageIds).associateBy { it.id }

        val data =
            trimmed.map { conv ->
                ConversationResponse.of(
                    conversation = conv,
                    partner = partners[conv.partnerId],
                    lastMessage = messages[conv.lastMessageId]!!,
                )
            }

        val nextCursor = if (hasNext) data.last().lastMessage.messageId else null

        return CursorResponse(
            data = data,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    @Transactional
    fun updateConversations(message: Message) {
        // Deadlock 방지: userId 기준 정렬하여 항상 같은 순서로 락 획득
        if (message.senderId < message.receiverId) {
            upsertSenderConversation(message)
            upsertReceiverConversation(message)
        } else {
            upsertReceiverConversation(message)
            upsertSenderConversation(message)
        }
    }

    @Transactional
    fun readMessages(
        partnerId: UUID,
        user: User,
    ) {
        messageRepository.markAllAsRead(
            senderId = partnerId,
            receiverId = user.id,
            readAt = Instant.now(),
        )

        val lastReadMessageId = messageRepository.findLastReadMessageId(
            senderId = partnerId,
            receiverId = user.id,
        ) ?: return

        conversationRepository.markAsRead(
            userId = user.id,
            partnerId = partnerId,
            lastReadMessageId = lastReadMessageId,
        )
    }

    private fun upsertSenderConversation(message: Message) {
        val updated =
            conversationRepository.updateLastMessageId(
                userId = message.senderId,
                partnerId = message.receiverId,
                messageId = message.id,
            )
        if (updated == 0) {
            conversationRepository.save(
                Conversation(
                    userId = message.senderId,
                    partnerId = message.receiverId,
                    lastMessageId = message.id,
                ),
            )
        }
    }

    private fun upsertReceiverConversation(message: Message) {
        val updated =
            conversationRepository.updateLastMessageAndIncrementUnreadCount(
                userId = message.receiverId,
                partnerId = message.senderId,
                messageId = message.id,
            )
        if (updated == 0) {
            val conversation =
                Conversation(
                    userId = message.receiverId,
                    partnerId = message.senderId,
                    lastMessageId = message.id,
                )
            conversation.unreadCount = 1
            conversationRepository.save(conversation)
        }
    }
}
