package io.waggle.waggleapiserver.domain.notification.repository

import io.waggle.waggleapiserver.domain.notification.Notification
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun countByUserId(userId: UUID): Long

    fun countByUserIdAndReadAtIsNull(userId: UUID): Long

    @Query(
        """
        SELECT n FROM Notification n
        WHERE n.userId = :userId
        AND (:cursor IS NULL OR n.id < :cursor)
        ORDER BY n.id DESC
        """,
    )
    fun findByUserIdWithCursor(
        @Param("userId") userId: UUID,
        @Param("cursor") cursor: Long?,
        pageable: Pageable,
    ): List<Notification>

    @Modifying
    @Query(
        """
        UPDATE Notification n SET n.readAt = :now
        WHERE n.userId = :userId AND n.id IN :ids AND n.readAt IS NULL
        """,
    )
    fun markAsReadByIds(
        @Param("userId") userId: UUID,
        @Param("ids") ids: List<Long>,
        @Param("now") now: Instant,
    )

    @Modifying
    @Query(
        """
        UPDATE Notification n SET n.readAt = :now
        WHERE n.userId = :userId AND n.readAt IS NULL
        """,
    )
    fun markAllAsRead(
        @Param("userId") userId: UUID,
        @Param("now") now: Instant,
    )
}
