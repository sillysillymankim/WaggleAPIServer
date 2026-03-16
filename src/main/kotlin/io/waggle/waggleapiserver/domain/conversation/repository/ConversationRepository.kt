package io.waggle.waggleapiserver.domain.conversation.repository

import io.waggle.waggleapiserver.domain.conversation.Conversation
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.util.UUID

interface ConversationRepository : JpaRepository<Conversation, Long> {
    @Query(
        """
        SELECT c FROM Conversation c
        WHERE c.userId = :userId
        AND (:cursor IS NULL OR c.lastMessageId < :cursor)
        ORDER BY c.lastMessageId DESC
    """,
    )
    fun findByUserId(
        userId: UUID,
        cursor: Long?,
        pageable: Pageable,
    ): List<Conversation>

    @Modifying
    @Query(
        """
        UPDATE Conversation c
        SET c.lastMessageId = :messageId,
            c.unreadCount = c.unreadCount + 1
        WHERE c.userId = :userId AND c.partnerId = :partnerId
    """,
    )
    fun updateLastMessageAndIncrementUnreadCount(
        userId: UUID,
        partnerId: UUID,
        messageId: Long,
    ): Int

    @Modifying
    @Query(
        """
        UPDATE Conversation c
        SET c.lastMessageId = :messageId
        WHERE c.userId = :userId AND c.partnerId = :partnerId
    """,
    )
    fun updateLastMessageId(
        userId: UUID,
        partnerId: UUID,
        messageId: Long,
    ): Int

    @Modifying
    @Query(
        """
        UPDATE Conversation c
        SET c.unreadCount = 0,
            c.lastReadMessageId = :lastReadMessageId
        WHERE c.userId = :userId AND c.partnerId = :partnerId
    """,
    )
    fun markAsRead(
        userId: UUID,
        partnerId: UUID,
        lastReadMessageId: Long,
    ): Int
}
