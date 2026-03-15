package io.waggle.waggleapiserver.domain.notification.repository

import io.waggle.waggleapiserver.domain.notification.Notification
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface NotificationRepository : JpaRepository<Notification, Long> {
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
}
