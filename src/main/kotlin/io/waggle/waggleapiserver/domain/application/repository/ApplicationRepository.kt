package io.waggle.waggleapiserver.domain.application.repository

import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.user.enums.Position
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface ApplicationRepository : JpaRepository<Application, Long> {
    fun existsByTeamIdAndUserIdAndPosition(
        teamId: Long,
        userId: UUID,
        position: Position,
    ): Boolean

    fun findByIdAndUserId(
        id: Long,
        userId: UUID,
    ): Application?

    fun findByUserId(userId: UUID): List<Application>

    fun findByStatusAndCreatedAtBetween(
        status: ApplicationStatus,
        createdAtStart: Instant,
        createdAtEnd: Instant,
    ): List<Application>

    @Query(
        """
        SELECT a FROM Application a
        WHERE a.teamId = :teamId
        AND (:cursor IS NULL OR a.id < :cursor)
        ORDER BY a.id DESC
        """,
    )
    fun findByTeamIdWithCursor(
        @Param("teamId") teamId: Long,
        @Param("cursor") cursor: Long?,
        pageable: Pageable,
    ): List<Application>

    @Query(
        """
        SELECT a FROM Application a
        WHERE a.postId = :postId
        AND (:cursor IS NULL OR a.id < :cursor)
        ORDER BY a.id DESC
        """,
    )
    fun findByPostIdWithCursor(
        @Param("postId") postId: Long,
        @Param("cursor") cursor: Long?,
        pageable: Pageable,
    ): List<Application>

    @Query("SELECT a.postId AS postId, COUNT(a) AS applicantCount FROM Application a WHERE a.postId IN :postIds GROUP BY a.postId")
    fun countApplicantsGroupByPostId(postIds: List<Long>): List<PostApplicantCount>

    @Query(
        """
        SELECT a.postId AS postId, COUNT(a) AS unreadCount
        FROM Application a
        WHERE a.postId IN :postIds
        AND NOT EXISTS (
            SELECT 1 FROM ApplicationRead ar
            WHERE ar.applicationId = a.id
            AND ar.userId = :userId
        )
        GROUP BY a.postId
        """,
    )
    fun countUnreadApplicationsGroupByPostId(
        @Param("userId") userId: UUID,
        @Param("postIds") postIds: List<Long>,
    ): List<PostUnreadCount>
}

interface PostApplicantCount {
    val postId: Long
    val applicantCount: Long
}

interface PostUnreadCount {
    val postId: Long
    val unreadCount: Long
}
