package io.waggle.waggleapiserver.domain.message.repository

import io.waggle.waggleapiserver.domain.message.Message
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface MessageRepository : JpaRepository<Message, Long> {
    @Query(
        """
        SELECT MAX(m.id) FROM Message m
        WHERE m.senderId = :senderId AND m.receiverId = :receiverId
        AND m.readAt IS NOT NULL
        """,
    )
    fun findLastReadMessageId(
        senderId: UUID,
        receiverId: UUID,
    ): Long?

    @Query(
        """
        SELECT m FROM Message m
        WHERE ((m.senderId = :userId AND m.receiverId = :partnerId)
            OR (m.senderId = :partnerId AND m.receiverId = :userId))
        AND (:cursor IS NULL OR m.id < :cursor)
        ORDER BY m.id DESC
        """,
    )
    fun findMessageHistoryBefore(
        userId: UUID,
        partnerId: UUID,
        cursor: Long?,
        pageable: Pageable,
    ): List<Message>

    @Query(
        """
        SELECT m FROM Message m
        WHERE ((m.senderId = :userId AND m.receiverId = :partnerId)
            OR (m.senderId = :partnerId AND m.receiverId = :userId))
        AND (:cursor IS NULL OR m.id > :cursor)
        ORDER BY m.id ASC
        """,
    )
    fun findMessageHistoryAfter(
        userId: UUID,
        partnerId: UUID,
        cursor: Long?,
        pageable: Pageable,
    ): List<Message>

    @Modifying
    @Query(
        """
        UPDATE Message m
        SET m.readAt = :readAt
        WHERE m.senderId = :senderId AND m.receiverId = :receiverId
        AND m.readAt IS NULL
        """,
    )
    fun markAllAsRead(
        senderId: UUID,
        receiverId: UUID,
        readAt: Instant,
    ): Int
}
